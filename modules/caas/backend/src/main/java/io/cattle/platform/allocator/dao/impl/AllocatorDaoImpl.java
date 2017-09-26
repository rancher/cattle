package io.cattle.platform.allocator.dao.impl;

import io.cattle.platform.allocator.dao.AllocatorDao;
import io.cattle.platform.allocator.port.PortManager;
import io.cattle.platform.allocator.service.AllocationAttempt;
import io.cattle.platform.allocator.service.AllocationCandidate;
import io.cattle.platform.core.constants.HealthcheckConstants;
import io.cattle.platform.core.constants.HostConstants;
import io.cattle.platform.core.constants.VolumeConstants;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.StoragePool;
import io.cattle.platform.core.model.Volume;
import io.cattle.platform.core.model.tables.records.HostRecord;
import io.cattle.platform.core.model.tables.records.InstanceRecord;
import io.cattle.platform.core.util.PortSpec;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.deferred.util.DeferredUtils;
import io.cattle.platform.eventing.EventService;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.object.util.ObjectUtils;
import io.github.ibuildthecloud.gdapi.util.TransactionDelegate;
import org.jooq.Configuration;
import org.jooq.Record1;
import org.jooq.RecordHandler;
import org.jooq.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static io.cattle.platform.core.model.tables.HostTable.*;
import static io.cattle.platform.core.model.tables.InstanceTable.*;
import static io.cattle.platform.core.model.tables.MountTable.*;
import static io.cattle.platform.core.model.tables.StoragePoolHostMapTable.*;
import static io.cattle.platform.core.model.tables.VolumeTable.*;

public class AllocatorDaoImpl extends AbstractJooqDao implements AllocatorDao {

    private static final Logger log = LoggerFactory.getLogger(AllocatorDaoImpl.class);

    ObjectManager objectManager;
    TransactionDelegate transaction;
    EventService eventService;

    public AllocatorDaoImpl(Configuration configuration, ObjectManager objectManager, TransactionDelegate transaction,
                            EventService eventService) {
        super(configuration);
        this.objectManager = objectManager;
        this.transaction = transaction;
        this.eventService = eventService;
    }

    @Override
    public List<? extends Host> getHosts(Collection<? extends StoragePool> pools) {
        if (pools == null || pools.isEmpty()) {
            return new ArrayList<>();
        }

        Collection<Long> poolIds = new HashSet<>();
        for (StoragePool p : pools) {
            poolIds.add(p.getId());
        }

        return create()
                .select(HOST.fields())
                .from(HOST)
                .join(STORAGE_POOL_HOST_MAP)
                    .on(STORAGE_POOL_HOST_MAP.HOST_ID.eq(HOST.ID))
                .where(STORAGE_POOL_HOST_MAP.REMOVED.isNull()
                    .and(STORAGE_POOL_HOST_MAP.STORAGE_POOL_ID.in(poolIds)))
                .fetchInto(HostRecord.class);
    }

    @Override
    public boolean recordCandidate(AllocationAttempt attempt, AllocationCandidate candidate, PortManager portManager) {
        Boolean result = false;
        try {
            result = DeferredUtils.nest(() -> {
                transaction.doInTransaction(() -> {
                    Long newHost = candidate.getHost();
                    if (newHost != null) {
                        for (Instance instance : attempt.getInstances()) {
                            log.info("Associating instance [{}] to host [{}]", instance.getId(), newHost);
                            instance.setHostId(newHost);
                            objectManager.persist(instance);
                            ObjectUtils.publishChanged(eventService, instance.getAccountId(), instance.getClusterId(), newHost, HostConstants.TYPE);
                        }

                        updateVolumeHostInfo(attempt, candidate, newHost);
                    }
                });
                return true;
            });
            return result;
        } finally {
            if (result && candidate.getHost() != null) {
                for (Instance instance : attempt.getInstances()) {
                    portManager.assignPorts(candidate.getClusterId(), candidate.getHost(), instance.getId(), PortSpec.getPorts(instance));
                }
            }
        }
    }

    void updateVolumeHostInfo(AllocationAttempt attempt, AllocationCandidate candidate, Long newHost) {
        for (Volume v : attempt.getVolumes()) {
            boolean persist = false;
            if (VolumeConstants.ACCESS_MODE_SINGLE_HOST_RW.equals(v.getAccessMode())) {
                persist = true;
                DataAccessor.fromDataFieldOf(v).withKey(VolumeConstants.FIELD_LAST_ALLOCATED_HOST_ID).set(newHost);
            }
            if (persist) {
                objectManager.persist(v);
            }
        }
    }

    @Override
    public void releaseAllocation(Instance instance) {
        transaction.doInTransaction(() -> {
            //Reload for persisting
            instance.setHostId(null);
            objectManager.persist(instance);
        });
    }

    @Override
    public List<Long> getInstancesWithVolumeMounted(long volumeId, long currentInstanceId) {
        return create()
                .select(INSTANCE.ID)
                .from(INSTANCE)
                .join(MOUNT)
                    .on(MOUNT.INSTANCE_ID.eq(INSTANCE.ID).and(MOUNT.VOLUME_ID.eq(volumeId)))
                .where(INSTANCE.REMOVED.isNull()
                    .and(INSTANCE.ID.ne(currentInstanceId))
                    .and(INSTANCE.HOST_ID.isNotNull())
                    .and((INSTANCE.HEALTH_STATE.isNull().or(INSTANCE.HEALTH_STATE.eq(HealthcheckConstants.HEALTH_STATE_HEALTHY)))))
                .fetchInto(Long.class);
    }

    @Override
    public Set<Long> findHostsWithVolumeInUse(long volumeId) {
        Set<Long> result = new HashSet<>();
        create()
            .select(HOST.ID)
            .from(INSTANCE)
            .join(MOUNT)
                .on(MOUNT.INSTANCE_ID.eq(INSTANCE.ID).and(MOUNT.VOLUME_ID.eq(volumeId)))
            .where(INSTANCE.REMOVED.isNull()
                .and(INSTANCE.HOST_ID.isNotNull())
                .and((INSTANCE.HEALTH_STATE.isNull().or(INSTANCE.HEALTH_STATE.eq(HealthcheckConstants.HEALTH_STATE_HEALTHY)))))
            .fetchInto((RecordHandler<Record1<Long>>) record ->
                    result.add(record.getValue(HOST.ID)));
        return result;
    }

    @Override
    public List<Instance> getUnmappedDeploymentUnitInstances(Long deploymentUnitId) {
        List<? extends Instance> instanceRecords = create()
                .select(INSTANCE.fields())
                .from(INSTANCE)
                .where(INSTANCE.REMOVED.isNull())
                .and(INSTANCE.DEPLOYMENT_UNIT_ID.eq(deploymentUnitId))
                .and(INSTANCE.DESIRED.isTrue())
                .and(INSTANCE.HOST_ID.isNull())
                .fetchInto(InstanceRecord.class);

        return new ArrayList<>(instanceRecords);
    }

    @Override
    public Long getHostAffinityForVolume(Volume volume) {
        Result<Record1<Long>> result = create().select(STORAGE_POOL_HOST_MAP.HOST_ID)
            .from(STORAGE_POOL_HOST_MAP)
            .join(VOLUME)
                .on(VOLUME.STORAGE_POOL_ID.eq(STORAGE_POOL_HOST_MAP.STORAGE_POOL_ID))
            .where(VOLUME.ID.eq(volume.getId())
                        .and(STORAGE_POOL_HOST_MAP.REMOVED.isNull()))
            .limit(2)
            .fetch();

        return result.size() == 1 ? result.get(0).getValue(STORAGE_POOL_HOST_MAP.HOST_ID) : null;
    }

}
