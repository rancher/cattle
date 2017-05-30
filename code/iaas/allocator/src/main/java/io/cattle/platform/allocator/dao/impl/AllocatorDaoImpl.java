package io.cattle.platform.allocator.dao.impl;

import static io.cattle.platform.core.model.tables.AgentTable.*;
import static io.cattle.platform.core.model.tables.HostLabelMapTable.*;
import static io.cattle.platform.core.model.tables.HostTable.*;
import static io.cattle.platform.core.model.tables.ImageStoragePoolMapTable.*;
import static io.cattle.platform.core.model.tables.ImageTable.*;
import static io.cattle.platform.core.model.tables.InstanceHostMapTable.*;
import static io.cattle.platform.core.model.tables.InstanceLabelMapTable.*;
import static io.cattle.platform.core.model.tables.InstanceTable.*;
import static io.cattle.platform.core.model.tables.LabelTable.*;
import static io.cattle.platform.core.model.tables.MountTable.*;
import static io.cattle.platform.core.model.tables.PortTable.*;
import static io.cattle.platform.core.model.tables.ServiceExposeMapTable.*;
import static io.cattle.platform.core.model.tables.StorageDriverTable.*;
import static io.cattle.platform.core.model.tables.StoragePoolHostMapTable.*;
import static io.cattle.platform.core.model.tables.StoragePoolTable.*;
import static io.cattle.platform.core.model.tables.VolumeStoragePoolMapTable.*;

import io.cattle.platform.allocator.dao.AllocatorDao;
import io.cattle.platform.allocator.exception.FailedToAllocate;
import io.cattle.platform.allocator.service.AllocationAttempt;
import io.cattle.platform.allocator.service.AllocationCandidate;
import io.cattle.platform.allocator.util.AllocatorUtils;
import io.cattle.platform.core.constants.AgentConstants;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.HealthcheckConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.StorageDriverConstants;
import io.cattle.platform.core.constants.VolumeConstants;
import io.cattle.platform.core.dao.GenericMapDao;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.InstanceHostMap;
import io.cattle.platform.core.model.Port;
import io.cattle.platform.core.model.StorageDriver;
import io.cattle.platform.core.model.StoragePool;
import io.cattle.platform.core.model.Volume;
import io.cattle.platform.core.model.VolumeStoragePoolMap;
import io.cattle.platform.core.model.tables.records.HostRecord;
import io.cattle.platform.core.model.tables.records.InstanceRecord;
import io.cattle.platform.core.model.tables.records.PortRecord;
import io.cattle.platform.core.model.tables.records.StoragePoolRecord;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.DataAccessor;
import io.github.ibuildthecloud.gdapi.condition.ConditionType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.lang.StringUtils;
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Record1;
import org.jooq.Record3;
import org.jooq.RecordHandler;
import org.jooq.Result;
import org.jooq.SelectConditionStep;
import org.jooq.exception.InvalidResultException;
import org.jooq.impl.DSL;
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

    @Override
    public boolean isInstanceImageKind(long instanceId, String kind) {
        return create().select(STORAGE_POOL.fields())
                .from(STORAGE_POOL)
                .join(IMAGE_STORAGE_POOL_MAP)
                    .on(STORAGE_POOL.ID.eq(IMAGE_STORAGE_POOL_MAP.STORAGE_POOL_ID))
                .join(IMAGE)
                    .on(IMAGE.ID.eq(IMAGE_STORAGE_POOL_MAP.IMAGE_ID))
                .join(INSTANCE)
                    .on(INSTANCE.IMAGE_ID.eq(IMAGE.ID))
                .where(
                    INSTANCE.ID.eq(instanceId)
                    .and(IMAGE_STORAGE_POOL_MAP.REMOVED.isNull())
                    .and(STORAGE_POOL.KIND.eq(kind)))
                .fetch().size() > 0;
    }

    @Override
    public boolean isVolumeInstanceImageKind(long volumeId, String kind) {
        Volume volume = objectManager.loadResource(Volume.class, volumeId);
        Long instanceId = volume.getInstanceId();

        return instanceId == null ? false : isInstanceImageKind(instanceId, kind);
    }

    @Override
    public List<? extends StoragePool> getAssociatedPools(Volume volume) {
        return create()
                .select(STORAGE_POOL.fields())
                .from(STORAGE_POOL)
                .join(VOLUME_STORAGE_POOL_MAP)
                    .on(VOLUME_STORAGE_POOL_MAP.STORAGE_POOL_ID.eq(STORAGE_POOL.ID))
                .where(
                    VOLUME_STORAGE_POOL_MAP.REMOVED.isNull()
                    .and(VOLUME_STORAGE_POOL_MAP.VOLUME_ID.eq(volume.getId())))
                .fetchInto(StoragePoolRecord.class);
    }

    @Override
    public List<? extends StoragePool> getAssociatedUnmanagedPools(Host host) {
        return create()
                .select(STORAGE_POOL.fields())
                .from(STORAGE_POOL)
                .join(STORAGE_POOL_HOST_MAP)
                    .on(STORAGE_POOL_HOST_MAP.STORAGE_POOL_ID.eq(STORAGE_POOL.ID))
                .where(
                    STORAGE_POOL_HOST_MAP.REMOVED.isNull()
                    .and(STORAGE_POOL_HOST_MAP.HOST_ID.eq(host.getId())
                    .and(STORAGE_POOL.KIND.in(AllocatorUtils.UNMANGED_STORAGE_POOLS))))
                .fetchInto(StoragePoolRecord.class);
    }

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
        Long newHost = candidate.getHost();
        if (newHost != null) {
            for (Instance instance : attempt.getInstances()) {
                log.info("Associating instance [{}] to host [{}]", instance.getId(), newHost);
                objectManager.create(InstanceHostMap.class,
                        INSTANCE_HOST_MAP.HOST_ID, newHost,
                        INSTANCE_HOST_MAP.INSTANCE_ID, instance.getId());
            }

            updateVolumeHostInfo(attempt, candidate, newHost);
        }

        Map<Long, Set<Long>> existingPools = attempt.getPoolIds();
        Map<Long, Set<Long>> newPools = candidate.getPools();

        if (!existingPools.keySet().equals(newPools.keySet())) {
            throw new IllegalStateException(String.format("Volumes don't match. currently %s, new %s", existingPools.keySet(), newPools.keySet()));
        }

        for (Map.Entry<Long, Set<Long>> entry : newPools.entrySet()) {
            long volumeId = entry.getKey();
            Set<Long> existingPoolsForVol = existingPools.get(entry.getKey());
            Set<Long> newPoolsForVol = entry.getValue();
            if (existingPoolsForVol == null || existingPoolsForVol.size() == 0) {
                for (long poolId : newPoolsForVol) {
                    log.info("Associating volume [{}] to storage pool [{}]", volumeId, poolId);
                    objectManager.create(VolumeStoragePoolMap.class,
                            VOLUME_STORAGE_POOL_MAP.VOLUME_ID, volumeId,
                            VOLUME_STORAGE_POOL_MAP.STORAGE_POOL_ID, poolId);
                }
            } else if (!existingPoolsForVol.equals(newPoolsForVol)) {
                throw new IllegalStateException(String.format("Can not move volume %s, currently: %s, new: %s", volumeId, existingPools, newPools));
            }
        }
        if (attempt.getAllocatedIPs() != null) {
            updateInstancePorts(attempt.getAllocatedIPs());
        }

        return true;
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
            StorageDriver d = v.getStorageDriverId() != null ? storageDrivers.get(v.getStorageDriverId()) : null;
            if (d != null && StorageDriverConstants.SCOPE_LOCAL.equals(DataAccessor.fieldString(d, StorageDriverConstants.FIELD_SCOPE))) {
                persist = true;
                getAllocatedHostUuidProp(v).set(candidate.getHostUuid());
            }
            if (VolumeConstants.ACCESS_MODE_SINGLE_HOST_RW.equals(v.getAccessMode())) {
                persist = true;
                DataAccessor.fromDataFieldOf(v).withKey(VolumeConstants.FIELD_LAST_ALLOCATED_HOST_ID).set(newHost);
            }
            if (persist) {
                objectManager.persist(v);
            }
        }
    }

    protected boolean isEmtpy(Map<Long,Set<Long>> set) {
        for ( Set<Long> value : set.values() ) {
            if ( value.size() > 0 ) {
                return false;
            }
        }

        return true;
    }

    @Override
    public void releaseAllocation(Instance instance,  InstanceHostMap map) {
        //Reload for persisting
        map = objectManager.loadResource(InstanceHostMap.class, map.getId());
        DataAccessor data = getDeallocatedProp(map);
        Boolean done = data.as(Boolean.class);
        if ( done == null || ! done.booleanValue() ) {
            data.set(true);
            objectManager.persist(map);
        }
    }

    @Override
    public void releaseAllocation(Volume volume) {
        //Reload for persisting
        volume = objectManager.reload(volume);
        DataAccessor data = getDeallocatedProp(volume);
        Boolean done = data.as(Boolean.class);
        if ( done == null || ! done.booleanValue() ) {
            data.set(true);
            objectManager.persist(volume);
        }
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
    public String getAllocatedHostUuid(Volume volume) {
        return getAllocatedHostUuidProp(volume).as(String.class);
    }

    protected DataAccessor getAllocatedHostUuidProp(Volume v) {
        return DataAccessor.fromDataFieldOf(v)
        .withScope(AllocatorDao.class)
        .withKey("allocatedHostUuid");
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
    public List<Port> getUsedPortsForHostExcludingInstance(long hostId, long instanceId) {
        return create()
                .select(PORT.fields())
                    .from(PORT)
                    .join(INSTANCE_HOST_MAP)
                        .on(PORT.INSTANCE_ID.eq(INSTANCE_HOST_MAP.INSTANCE_ID))
                    .join(INSTANCE)
                        .on(INSTANCE_HOST_MAP.INSTANCE_ID.eq(INSTANCE.ID))
                .leftOuterJoin(SERVICE_EXPOSE_MAP)
                .on(SERVICE_EXPOSE_MAP.INSTANCE_ID.eq(INSTANCE.ID))
                    .where(INSTANCE_HOST_MAP.HOST_ID.eq(hostId)
                        .and(INSTANCE.REMOVED.isNull())
                        .and(INSTANCE.ID.ne(instanceId))
                        .and(INSTANCE.STATE.in(InstanceConstants.STATE_STARTING, InstanceConstants.STATE_RESTARTING, InstanceConstants.STATE_RUNNING))
                        .and(INSTANCE_HOST_MAP.REMOVED.isNull())
                        .and(PORT.REMOVED.isNull())
                        .and(SERVICE_EXPOSE_MAP.UPGRADE.eq(false).or(SERVICE_EXPOSE_MAP.UPGRADE.isNull())))
                .fetchInto(Port.class);
    }

    @Override
    public Map<String, String[]> getLabelsForHost(long hostId) {
        final Map<String, String[]> labelKeyValueStatusMap = new HashMap<>();

        create()
            .select(LABEL.KEY, LABEL.VALUE, HOST_LABEL_MAP.STATE)
                .from(LABEL)
                .join(HOST_LABEL_MAP)
                    .on(LABEL.ID.eq(HOST_LABEL_MAP.LABEL_ID))
                .where(HOST_LABEL_MAP.HOST_ID.eq(hostId))
                    .and(LABEL.REMOVED.isNull())
                    .and(HOST_LABEL_MAP.REMOVED.isNull())
            .fetchInto(new RecordHandler<Record3<String, String, String>>() {
                @Override
                public void next(Record3<String, String, String> record) {
                    labelKeyValueStatusMap.put(StringUtils.lowerCase(record.value1()),
                            new String[] {
                                StringUtils.lowerCase(record.value2()),
                                record.value3()
                                });
                }
            });

        return labelKeyValueStatusMap;
    }

    @Override
    public List<? extends Host> getNonRemovedHosts(long accountId) {
        return create()
                .select(HOST.fields())
                .from(HOST)
                .where(HOST.ACCOUNT_ID.eq(accountId)
                .and(HOST.REMOVED.isNull()))
                .fetchInto(Host.class);
    }

    @Override
    public List<? extends Host> getActiveHosts(long accountId) {
        return create()
                .select(HOST.fields())
                .from(HOST)
                .leftOuterJoin(AGENT)
                    .on(AGENT.ID.eq(HOST.AGENT_ID))
                .where(
                AGENT.ID.isNull()
                .or(AGENT.STATE.in(CommonStatesConstants.ACTIVE, AgentConstants.STATE_FINISHING_RECONNECT, AgentConstants.STATE_RECONNECTED))
                .and(HOST.REMOVED.isNull())
                .and(HOST.ACCOUNT_ID.eq(accountId))
                .and(HOST.STATE.in(CommonStatesConstants.ACTIVATING, CommonStatesConstants.ACTIVE, CommonStatesConstants.UPDATING_ACTIVE)))
                .fetchInto(Host.class);
    }

    @Override
    public boolean hostHasContainerLabel(long hostId, String labelKey,
            String labelValue) {
        return create()
                .select(LABEL.ID)
                    .from(LABEL)
                    .join(INSTANCE_LABEL_MAP)
                        .on(LABEL.ID.eq(INSTANCE_LABEL_MAP.LABEL_ID))
                    .join(INSTANCE_HOST_MAP)
                        .on(INSTANCE_LABEL_MAP.INSTANCE_ID.eq(INSTANCE_HOST_MAP.INSTANCE_ID))
                    .join(INSTANCE)
                        .on(INSTANCE_HOST_MAP.INSTANCE_ID.eq(INSTANCE.ID))
                .leftOuterJoin(SERVICE_EXPOSE_MAP)
                .on(SERVICE_EXPOSE_MAP.INSTANCE_ID.eq(INSTANCE.ID))
                    .where(INSTANCE_HOST_MAP.HOST_ID.eq(hostId))
                        .and(LABEL.REMOVED.isNull())
                        .and(INSTANCE_LABEL_MAP.REMOVED.isNull())
                        .and(INSTANCE_HOST_MAP.REMOVED.isNull())
                        .and(INSTANCE.REMOVED.isNull())
                .and(INSTANCE.STATE.notIn(InstanceConstants.STATE_ERROR, InstanceConstants.STATE_ERRORING))
                        .and(LABEL.KEY.equalIgnoreCase(labelKey))
                        .and(LABEL.VALUE.equalIgnoreCase(labelValue))
                .and(SERVICE_EXPOSE_MAP.UPGRADE.eq(false).or(SERVICE_EXPOSE_MAP.UPGRADE.isNull()))
                .fetchInto(Long.class).size() > 0;

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
        List<CandidateHostInfo> hostInfos = new ArrayList<>();
        Set<Long> hostIds = new HashSet<>();
        if (orderedHostUuids == null) {
            Result<Record3<String, Long, Long>> result = getHostQuery(null, options).fetch();
            Collections.shuffle(result);

            Map<Long, CandidateHostInfo> infoMap = new HashMap<>();
            for (Record3<String, Long, Long> r : result) {
                Long hostId = r.value2();
                hostIds.add(hostId);
                CandidateHostInfo hostInfo = infoMap.get(hostId);
                if (hostInfo == null) {
                    hostInfo = new CandidateHostInfo(hostId, r.value1());
                    infoMap.put(hostId, hostInfo);
                    hostInfos.add(hostInfo);
                }
                hostInfo.getPoolIds().add(r.value3());
            }
        } else {
            Map<String, Result<Record3<String, Long, Long>>> result = getHostQuery(orderedHostUuids, options).fetchGroups(HOST.UUID);
            for (String uuid : orderedHostUuids) {
                Result<Record3<String, Long, Long>>val = result.get(uuid);
                if (val != null) {
                    Set<Long>poolIds = new HashSet<>();
                    Long hostId = null;
                    for (Record3<String, Long, Long>r : val) {
                        poolIds.add(r.value3());
                        // host id is same in all records for this group
                        if (hostId == null) {
                            hostId = r.value2();
                        }
                    }

                    CandidateHostInfo hostInfo = new CandidateHostInfo(hostId, uuid);
                    hostInfo.getPoolIds().addAll(poolIds);
                    hostInfos.add(hostInfo);
                    hostIds.add(hostId);
                }
            }
        }

        if (options.isIncludeUsedPorts()) {
            updateHostsWithUsedPorts(hostIds, hostInfos);
        }

        return new AllocationCandidateIterator(objectManager, hostInfos, volumes);
    }

    private void updateHostsWithUsedPorts(Set<Long> hostIds, List<CandidateHostInfo> hostInfos) {
        Map<Long, Result<Record>> results = create()
                .select(hostAndPortFields)
                    .from(PORT)
                    .join(INSTANCE_HOST_MAP)
                        .on(PORT.INSTANCE_ID.eq(INSTANCE_HOST_MAP.INSTANCE_ID))
                    .join(INSTANCE)
                        .on(INSTANCE_HOST_MAP.INSTANCE_ID.eq(INSTANCE.ID))
                .leftOuterJoin(SERVICE_EXPOSE_MAP)
                .on(SERVICE_EXPOSE_MAP.INSTANCE_ID.eq(INSTANCE.ID))
                    .where(INSTANCE_HOST_MAP.HOST_ID.in(hostIds)
                        .and(INSTANCE.REMOVED.isNull())
                        .and(INSTANCE.STATE.in(InstanceConstants.STATE_STARTING, InstanceConstants.STATE_RESTARTING, InstanceConstants.STATE_RUNNING))
                        .and(INSTANCE_HOST_MAP.REMOVED.isNull())
                        .and(PORT.REMOVED.isNull())
                        .and(SERVICE_EXPOSE_MAP.UPGRADE.eq(false).or(SERVICE_EXPOSE_MAP.UPGRADE.isNull())))
                .fetchGroups(INSTANCE_HOST_MAP.HOST_ID);

        Map<Long, List<Port>> hostToPorts = new HashMap<>();
        for (Map.Entry<Long, Result<Record>> entry : results.entrySet()) {
            List<Port> ports = new ArrayList<>();
            hostToPorts.put(entry.getKey(), ports);
            for (Record rec : entry.getValue()) {
                PortRecord port = rec.into(PortRecord.class);
                ports.add(port);
            }
        }

        for (CandidateHostInfo hostInfo : hostInfos) {
            List<Port> ports = hostToPorts.get(hostInfo.getHostId()) != null ? hostToPorts.get(hostInfo.getHostId()) : new ArrayList<>();
            hostInfo.setUsedPorts(ports);
        }
    }

    protected SelectConditionStep<Record3<String, Long, Long>> getHostQuery(List<String> orderedHostUUIDs, QueryOptions options) {
        return create()
                .select(HOST.UUID, HOST.ID, STORAGE_POOL.ID)
                .from(HOST)
                .leftOuterJoin(STORAGE_POOL_HOST_MAP)
                    .on(STORAGE_POOL_HOST_MAP.HOST_ID.eq(HOST.ID)
                        .and(STORAGE_POOL_HOST_MAP.REMOVED.isNull()))
                .join(STORAGE_POOL)
                    .on(STORAGE_POOL.ID.eq(STORAGE_POOL_HOST_MAP.STORAGE_POOL_ID))
                .leftOuterJoin(AGENT)
                    .on(AGENT.ID.eq(HOST.AGENT_ID))
                .where(getQueryOptionCondition(options, orderedHostUUIDs));
    }

    protected Condition inHostList(List<String> hostUUIDs) {
        if (hostUUIDs == null || hostUUIDs.isEmpty()) {
            return DSL.trueCondition();
        }
        return HOST.UUID.in(hostUUIDs);
    }

    protected Condition getQueryOptionCondition(QueryOptions options, List<String> orderedHostUUIDs) {
        Condition condition = null;

        if (options.getAccountId() != null) {
            condition = append(condition, HOST.ACCOUNT_ID.eq(options.getAccountId()));
        }

        if (options.getRequestedHostId() != null) {
            condition = append(condition, HOST.ID.eq(options.getRequestedHostId()));
            return condition;
        }

        condition = append(condition, AGENT.ID.isNull().or(AGENT.STATE.eq(CommonStatesConstants.ACTIVE))
        .and(HOST.STATE.in(CommonStatesConstants.ACTIVE, CommonStatesConstants.UPDATING_ACTIVE))
        .and(STORAGE_POOL.STATE.eq(CommonStatesConstants.ACTIVE))
        .and(inHostList(orderedHostUUIDs)));

        if ( options.getHosts().size() > 0 ) {
            condition = append(condition, HOST.ID.in(options.getHosts()));
        }

        if ( options.getKind() != null ) {
            condition = append(condition,
                    HOST.KIND.eq(options.getKind()).and(STORAGE_POOL.KIND.eq(options.getKind())));
        }

        return condition == null ? DSL.trueCondition() : condition;
    }

    protected Condition append(Condition base, Condition next) {
        if ( base == null ) {
            return next;
        } else {
            return base.and(next);
        }
    }
}
