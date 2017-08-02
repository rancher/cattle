package io.cattle.platform.allocator.dao.impl;

import io.cattle.platform.allocator.dao.AllocatorDao;
import io.cattle.platform.allocator.service.AllocationAttempt;
import io.cattle.platform.allocator.service.AllocationCandidate;
import io.cattle.platform.core.addon.PortInstance;
import io.cattle.platform.core.constants.HealthcheckConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.VolumeConstants;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.StorageDriver;
import io.cattle.platform.core.model.StoragePool;
import io.cattle.platform.core.model.Volume;
import io.cattle.platform.core.model.tables.records.HostRecord;
import io.cattle.platform.core.model.tables.records.InstanceRecord;
import io.cattle.platform.core.util.PortSpec;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.DataAccessor;
import io.github.ibuildthecloud.gdapi.util.ProxyUtils;
import io.github.ibuildthecloud.gdapi.util.TransactionDelegate;
import org.jooq.Configuration;
import org.jooq.Record1;
import org.jooq.RecordHandler;
import org.jooq.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static io.cattle.platform.core.model.tables.HostTable.*;
import static io.cattle.platform.core.model.tables.InstanceTable.*;
import static io.cattle.platform.core.model.tables.MountTable.*;
import static io.cattle.platform.core.model.tables.StorageDriverTable.*;
import static io.cattle.platform.core.model.tables.StoragePoolHostMapTable.*;
import static io.cattle.platform.core.model.tables.VolumeTable.*;
import static io.github.ibuildthecloud.gdapi.condition.Condition.*;

public class AllocatorDaoImpl extends AbstractJooqDao implements AllocatorDao {

    private static final Logger log = LoggerFactory.getLogger(AllocatorDaoImpl.class);

    private static final String INSTANCE_ID = "instanceID";
    private static final String ALLOCATED_IPS = "allocatedIPs";

    ObjectManager objectManager;
    TransactionDelegate transaction;

    public AllocatorDaoImpl(Configuration configuration, ObjectManager objectManager, TransactionDelegate transaction) {
        super(configuration);
        this.objectManager = objectManager;
        this.transaction = transaction;
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
    public boolean recordCandidate(AllocationAttempt attempt, AllocationCandidate candidate) {
        return transaction.doInTransactionResult(() -> {
            Long newHost = candidate.getHost();
            if (newHost != null) {
                for (Instance instance : attempt.getInstances()) {
                    log.info("Associating instance [{}] to host [{}]", instance.getId(), newHost);
                    instance.setHostId(newHost);
                    updateInstancePorts(instance, attempt.getAllocatedIPs());
                    objectManager.persist(instance);
                }

                updateVolumeHostInfo(attempt, candidate, newHost);
            }

            return true;
        });
    }

    void updateVolumeHostInfo(AllocationAttempt attempt, AllocationCandidate candidate, Long newHost) {
        List<Object> storageDriverIds = new ArrayList<>();
        for (Volume v : attempt.getVolumes()) {
            if (v.getStorageDriverId() != null) {
                storageDriverIds.add(v.getStorageDriverId());
            }
        }

        Map<Object, Object> criteria = new HashMap<>();
        criteria.put(STORAGE_DRIVER.REMOVED, isNull());
        criteria.put(STORAGE_DRIVER.ID, in(storageDriverIds));
        List<StorageDriver> drivers = objectManager.find(StorageDriver.class, criteria);
        Map<Long, StorageDriver> storageDrivers = new HashMap<>();
        for (StorageDriver d : drivers) {
            storageDrivers.put(d.getId(), d);
        }

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
            .fetchInto(new RecordHandler<Record1<Long>>() {
                @Override
                public void next(Record1<Long> record) {
                   result.add(record.getValue(HOST.ID));
                }
            });
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

        List<Instance> instances = new ArrayList<>();
        for (Instance i : instanceRecords) {
            instances.add(i);
        }
        return instances;
    }

    /* (non-Java doc)
     * @see io.cattle.platform.allocator.dao.AllocatorDao#updateInstancePorts(java.util.Map)
     * Scheduler will return a list of map showing the allocated result in the following format:
     * {
     *      "instanceID" : "xx",
     *      "allocatedIPs" : {
     *                              [
     *                                  {
     *                                      "allocatedIP" : "xxx.xxx.xxx.xxx",
     *                                      "publicPort" : "xxx",
     *                                      "privatePort" :  "xxx",
     *                                  },
     *                                  {
     *                                      "allocatedIP" : "xxx.xxx.xxx.xxx",
     *                                      "publicPort" : "xxx",
     *                                      "privatePort" :  "xxx",
     *                                  }
     *                              ]
     *                          }
     * }
     *
     * Then update instance ports with the bind address if needed.
     */
    private void updateInstancePorts(Instance instance, List<Map<String, Object>> dataList) {
        List<PortAssignment> portAssignments = getPortAssignments(instance, dataList);
        List<PortSpec> portSpecs = InstanceConstants.getPortSpecs(instance);
        List<PortInstance> portBindings = new ArrayList<>();

        for (PortSpec spec : portSpecs) {
            PortInstance binding = new PortInstance(spec);
            for (PortAssignment assignment : portAssignments) {
                if (matches(spec, assignment)) {
                    binding.setBindIpAddress(assignment.getAllocatedIP());
                }
            }
            portBindings.add(binding);
        }


        DataAccessor.setField(instance, InstanceConstants.FIELD_PORT_BINDINGS, portBindings);
    }

    private List<PortAssignment> getPortAssignments(Instance instance, List<Map<String, Object>> dataList) {
        if (dataList == null) {
            return Collections.emptyList();
        }

        for (Map<String, Object> data : dataList) {
            String instanceId = (String) data.get(INSTANCE_ID);
            if (!Objects.equals(instanceId, instance.getId().toString())) {
                continue;
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> allocatedIPList = (List<Map<String, Object>>) data.get(ALLOCATED_IPS);
            if (allocatedIPList == null) {
                continue;
            }
            return allocatedIPList.stream()
                    .map((x) -> ProxyUtils.proxy(x, PortAssignment.class))
                    .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }

    private boolean matches(PortSpec spec, PortAssignment assignment) {
        return spec.getIpAddress() == null &&
                Objects.equals(spec.getPublicPort(), assignment.getPublicPort()) &&
                Objects.equals(spec.getPrivatePort(), assignment.getPrivatePort()) &&
                Objects.equals(spec.getProtocol(), assignment.getProtocol());
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

    private interface PortAssignment {
        String getAllocatedIP();
        String getProtocol();
        Integer getPrivatePort();
        Integer getPublicPort();
    }

}
