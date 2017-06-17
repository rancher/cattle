package io.cattle.platform.allocator.dao.impl;

import static io.cattle.platform.core.model.tables.HostTable.*;
import static io.cattle.platform.core.model.tables.InstanceHostMapTable.*;
import static io.cattle.platform.core.model.tables.InstanceTable.*;
import static io.cattle.platform.core.model.tables.MountTable.*;
import static io.cattle.platform.core.model.tables.PortTable.*;
import static io.cattle.platform.core.model.tables.ServiceExposeMapTable.*;
import static io.cattle.platform.core.model.tables.StorageDriverTable.*;
import static io.cattle.platform.core.model.tables.StoragePoolHostMapTable.*;
import static io.cattle.platform.core.model.tables.VolumeStoragePoolMapTable.*;

import io.cattle.platform.allocator.dao.AllocatorDao;
import io.cattle.platform.allocator.exception.FailedToAllocate;
import io.cattle.platform.allocator.service.AllocationAttempt;
import io.cattle.platform.allocator.service.AllocationCandidate;
import io.cattle.platform.core.cache.EnvironmentResourceManager;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.HealthcheckConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.VolumeConstants;
import io.cattle.platform.core.dao.GenericMapDao;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.InstanceHostMap;
import io.cattle.platform.core.model.StorageDriver;
import io.cattle.platform.core.model.StoragePool;
import io.cattle.platform.core.model.Volume;
import io.cattle.platform.core.model.tables.records.HostRecord;
import io.cattle.platform.core.model.tables.records.InstanceRecord;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.DataAccessor;
import io.github.ibuildthecloud.gdapi.condition.ConditionType;
import io.github.ibuildthecloud.gdapi.util.TransactionDelegate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.sound.sampled.Port;

import org.apache.commons.lang.StringUtils;
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Record1;
import org.jooq.RecordHandler;
import org.jooq.Result;
import org.jooq.exception.InvalidResultException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AllocatorDaoImpl extends AbstractJooqDao implements AllocatorDao {

    private static final Logger log = LoggerFactory.getLogger(AllocatorDaoImpl.class);

    private static final String ALLOCATED_IP = "allocatedIP";
    private static final String PROTOCOL = "protocol";
    private static final String PRIVATE_PORT = "privatePort";
    private static final String PUBLIC_PORT = "publicPort";
    private static final String INSTANCE_ID = "instanceID";
    private static final String ALLOCATED_IPS = "allocatedIPs";
    private static final String BIND_ADDRESS = "bindAddress";

    private static final List<Field<?>> hostAndPortFields;
    static {
       hostAndPortFields = new ArrayList<>(Arrays.asList(PORT.fields()));
       hostAndPortFields.add(INSTANCE_HOST_MAP.HOST_ID);
    }

    static final List<String> IHM_STATES = Arrays.asList(new String[] { CommonStatesConstants.INACTIVE, CommonStatesConstants.DEACTIVATING,
            CommonStatesConstants.REMOVED, CommonStatesConstants.REMOVING });

    @Inject
    ObjectManager objectManager;
    @Inject
    GenericMapDao mapDao;
    @Inject
    TransactionDelegate transaction;
    @Inject
    EnvironmentResourceManager envResourceManager;

    @Override
    public Host getHost(Instance instance) {
       Condition cond = getInstanceHostConstraint(instance);
       try {
           return create()
                   .selectDistinct(HOST.fields())
                   .from(HOST)
                   .join(INSTANCE_HOST_MAP)
                       .on(INSTANCE_HOST_MAP.HOST_ID.eq(HOST.ID))
                   .join(INSTANCE)
                       .on(INSTANCE_HOST_MAP.INSTANCE_ID.eq(INSTANCE.ID))
                    .leftOuterJoin(SERVICE_EXPOSE_MAP)
                    .on(INSTANCE.ID.eq(SERVICE_EXPOSE_MAP.INSTANCE_ID))
                   .where(
                       INSTANCE_HOST_MAP.REMOVED.isNull()
                       .and(HOST.REMOVED.isNull())
                       .and(INSTANCE.REMOVED.isNull())
                                    .and(SERVICE_EXPOSE_MAP.REMOVED.isNull())
                                    .and(SERVICE_EXPOSE_MAP.UPGRADE.isNull().or(SERVICE_EXPOSE_MAP.UPGRADE.eq(false)))
                       .and(cond))
                   .fetchOneInto(HostRecord.class);
       } catch (InvalidResultException e) {
           throw new FailedToAllocate("Instances to allocate assigned to different hosts.");
       }
    }

    public Condition getInstanceHostConstraint(Instance instance) {
        if (StringUtils.isEmpty(instance.getDeploymentUnitUuid())) {
            return INSTANCE_HOST_MAP.INSTANCE_ID.eq(instance.getId());
        } else {
            return INSTANCE.DEPLOYMENT_UNIT_ID.eq(instance.getDeploymentUnitId()).and(
                    INSTANCE.STATE.notIn(InstanceConstants.STATE_ERROR, InstanceConstants.STATE_ERRORING));
        }
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
                    objectManager.create(InstanceHostMap.class,
                            INSTANCE_HOST_MAP.HOST_ID, newHost,
                            INSTANCE_HOST_MAP.STATE, CommonStatesConstants.INACTIVE,
                            INSTANCE_HOST_MAP.INSTANCE_ID, instance.getId());
                }

                updateVolumeHostInfo(attempt, candidate, newHost);
            }

            if (attempt.getAllocatedIPs() != null) {
                updateInstancePorts(attempt.getAllocatedIPs());
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
        criteria.put(STORAGE_DRIVER.REMOVED, new io.github.ibuildthecloud.gdapi.condition.Condition(ConditionType.NULL));
        criteria.put(STORAGE_DRIVER.ID, new io.github.ibuildthecloud.gdapi.condition.Condition(ConditionType.IN, storageDriverIds));
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
    public void releaseAllocation(Instance instance,  InstanceHostMap mapIn) {
        transaction.doInTransaction(() -> {
            //Reload for persisting
            InstanceHostMap map = objectManager.loadResource(InstanceHostMap.class, mapIn.getId());
            DataAccessor data = getDeallocatedProp(map);
            Boolean done = data.as(Boolean.class);
            if ( done == null || ! done.booleanValue() ) {
                data.set(true);
                objectManager.persist(map);
            }
        });
    }

    @Override
    public boolean isAllocationReleased(Object resource) {
        DataAccessor done = getDeallocatedProp(resource);
        return done.withDefault(false).as(Boolean.class);
    }

    private DataAccessor getDeallocatedProp(Object resource) {
        return DataAccessor.fromDataFieldOf(resource)
                .withScope(AllocatorDao.class)
                .withKey("deallocated");
    }

    @Override
    public Map<String, List<InstanceHostMap>> getInstanceHostMapsWithHostUuid(long instanceId) {
        Map<Record, List<InstanceHostMap>> result = create()
        .select(INSTANCE_HOST_MAP.fields()).select(HOST.UUID)
        .from(INSTANCE_HOST_MAP)
        .join(HOST).on(INSTANCE_HOST_MAP.HOST_ID.eq(HOST.ID))
        .where(INSTANCE_HOST_MAP.INSTANCE_ID.eq(instanceId)
        .and(INSTANCE_HOST_MAP.REMOVED.isNull()))
        .fetchGroups(new Field[]{HOST.UUID}, InstanceHostMap.class);

        Map<String, List<InstanceHostMap>> maps = new HashMap<>();
        for (Map.Entry<Record, List<InstanceHostMap>>entry : result.entrySet()) {
            String uuid = (String)entry.getKey().getValue(0);
            maps.put(uuid, entry.getValue());
        }
        return maps;
    }

    @Override
    public List<Long> getInstancesWithVolumeMounted(long volumeId, long currentInstanceId) {
        return create()
                .select(INSTANCE.ID)
                .from(INSTANCE)
                .join(MOUNT)
                    .on(MOUNT.INSTANCE_ID.eq(INSTANCE.ID).and(MOUNT.VOLUME_ID.eq(volumeId)))
                .join(INSTANCE_HOST_MAP)
                    .on(INSTANCE_HOST_MAP.INSTANCE_ID.eq(INSTANCE.ID))
                .where(INSTANCE.REMOVED.isNull()
                    .and(INSTANCE.ID.ne(currentInstanceId))
                    .and(INSTANCE_HOST_MAP.STATE.notIn(IHM_STATES))
                    .and((INSTANCE.HEALTH_STATE.isNull().or(INSTANCE.HEALTH_STATE.eq(HealthcheckConstants.HEALTH_STATE_HEALTHY)))))
                .fetchInto(Long.class);
    }

    @Override
    public boolean isVolumeInUseOnHost(long volumeId, long hostId) {
        return create()
                .select(INSTANCE.ID)
                .from(INSTANCE)
                .join(MOUNT)
                    .on(MOUNT.INSTANCE_ID.eq(INSTANCE.ID).and(MOUNT.VOLUME_ID.eq(volumeId)))
                .join(INSTANCE_HOST_MAP)
                    .on(INSTANCE_HOST_MAP.INSTANCE_ID.eq(INSTANCE.ID).and(INSTANCE_HOST_MAP.HOST_ID.eq(hostId)))
                .join(HOST)
                    .on(HOST.ID.eq(INSTANCE_HOST_MAP.HOST_ID))
                .where(INSTANCE.REMOVED.isNull()
                    .and(INSTANCE_HOST_MAP.STATE.notIn(IHM_STATES))
                    .and((INSTANCE.HEALTH_STATE.isNull().or(INSTANCE.HEALTH_STATE.eq(HealthcheckConstants.HEALTH_STATE_HEALTHY)))))
                .fetch().size() > 0;
    }

    @Override
    public Set<Long> findHostsWithVolumeInUse(long volumeId) {
        Set<Long> result = new HashSet<>();
        create()
            .select(HOST.ID)
            .from(INSTANCE)
            .join(MOUNT)
                .on(MOUNT.INSTANCE_ID.eq(INSTANCE.ID).and(MOUNT.VOLUME_ID.eq(volumeId)))
            .join(INSTANCE_HOST_MAP)
                .on(INSTANCE_HOST_MAP.INSTANCE_ID.eq(INSTANCE.ID))
            .join(HOST)
                .on(HOST.ID.eq(INSTANCE_HOST_MAP.HOST_ID))
            .where(INSTANCE.REMOVED.isNull()
                .and(INSTANCE_HOST_MAP.STATE.notIn(IHM_STATES))
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
                .leftOuterJoin(INSTANCE_HOST_MAP)
                    .on(INSTANCE_HOST_MAP.INSTANCE_ID.eq(INSTANCE.ID).and(INSTANCE_HOST_MAP.REMOVED.isNull()))
                .where(INSTANCE.REMOVED.isNull())
                .and(INSTANCE.DEPLOYMENT_UNIT_ID.eq(deploymentUnitId))
                .and(INSTANCE.DESIRED.isTrue())
                .and(INSTANCE_HOST_MAP.ID.isNull())
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
     * Then update
     * Port Table. Binding address field in port table and public port field in port table if public port is allocated by external scheduler (for agent)
     */
    @SuppressWarnings("unchecked")
    private void updateInstancePorts(List<Map<String, Object>> dataList) {
        for (Map<String, Object> data: dataList) {
            if (data.get(INSTANCE_ID) == null) {
                continue;
            }
            String instanceId = (String) data.get(INSTANCE_ID);
            if (data.get(ALLOCATED_IPS) == null) {
                continue;
            }

            List<Map<String, Object>> allocatedIPList = (List<Map<String, Object>>) data.get(ALLOCATED_IPS);
            Instance instance = objectManager.loadResource(Instance.class, instanceId);
            for (Map<String, Object> allocatedIp: allocatedIPList) {
                String ipAddress = (String) allocatedIp.get(ALLOCATED_IP);
                String protocol = (String) allocatedIp.get(PROTOCOL);
                Integer publicPort = (Integer) allocatedIp.get(PUBLIC_PORT);
                Integer privatePort = (Integer) allocatedIp.get(PRIVATE_PORT);
                for (Port port: objectManager.children(instance, Port.class)) {
                    if (port.getPrivatePort().equals(privatePort)
                            && StringUtils.equals(port.getProtocol(), protocol)
                            && (port.getPublicPort() == null || port.getPublicPort().equals(publicPort))) {
                        DataAccessor.setField(port, BIND_ADDRESS, ipAddress);
                        port.setPublicPort(publicPort);
                        objectManager.persist(port);
                        break;
                    }
                }
            }
        }
        return;
    }


    @Override
    public Iterator<AllocationCandidate> iteratorHosts(List<String> orderedHostUuids, List<Long> volumes, QueryOptions options) {
        return envResourceManager.iterateHosts(options, orderedHostUuids).map((x) -> {
            return new AllocationCandidate(x.getId(), x.getUuid(), x.getPorts(), options.getAccountId());
        }).iterator();
    }

    @Override
    public Long getHostAffinityForVolume(Volume volume) {
        Result<Record1<Long>> result = create().select(STORAGE_POOL_HOST_MAP.HOST_ID)
            .from(STORAGE_POOL_HOST_MAP)
            .join(VOLUME_STORAGE_POOL_MAP)
                .on(VOLUME_STORAGE_POOL_MAP.STORAGE_POOL_ID.eq(STORAGE_POOL_HOST_MAP.STORAGE_POOL_ID))
            .where(VOLUME_STORAGE_POOL_MAP.VOLUME_ID.eq(volume.getId())
                    .and(VOLUME_STORAGE_POOL_MAP.REMOVED.isNull()
                            .and(STORAGE_POOL_HOST_MAP.REMOVED.isNull())))
            .limit(2)
            .fetch();

        return result.size() == 1 ? result.get(0).getValue(STORAGE_POOL_HOST_MAP.HOST_ID) : null;
    }
}
