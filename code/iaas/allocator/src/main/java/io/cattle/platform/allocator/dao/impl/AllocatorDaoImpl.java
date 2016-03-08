package io.cattle.platform.allocator.dao.impl;

import static io.cattle.platform.core.model.tables.AgentTable.*;
import static io.cattle.platform.core.model.tables.HostLabelMapTable.*;
import static io.cattle.platform.core.model.tables.HostTable.*;
import static io.cattle.platform.core.model.tables.HostVnetMapTable.*;
import static io.cattle.platform.core.model.tables.ImageStoragePoolMapTable.*;
import static io.cattle.platform.core.model.tables.ImageTable.*;
import static io.cattle.platform.core.model.tables.InstanceHostMapTable.*;
import static io.cattle.platform.core.model.tables.InstanceLabelMapTable.*;
import static io.cattle.platform.core.model.tables.InstanceTable.*;
import static io.cattle.platform.core.model.tables.LabelTable.*;
import static io.cattle.platform.core.model.tables.NicTable.*;
import static io.cattle.platform.core.model.tables.PortTable.*;
import static io.cattle.platform.core.model.tables.ServiceExposeMapTable.*;
import static io.cattle.platform.core.model.tables.StoragePoolHostMapTable.*;
import static io.cattle.platform.core.model.tables.StoragePoolTable.*;
import static io.cattle.platform.core.model.tables.SubnetVnetMapTable.*;
import static io.cattle.platform.core.model.tables.VnetTable.*;
import static io.cattle.platform.core.model.tables.VolumeStoragePoolMapTable.*;
import io.cattle.platform.allocator.dao.AllocatorDao;
import io.cattle.platform.allocator.service.AllocationAttempt;
import io.cattle.platform.allocator.service.AllocationCandidate;
import io.cattle.platform.allocator.util.AllocatorUtils;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.dao.GenericMapDao;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.InstanceHostMap;
import io.cattle.platform.core.model.Nic;
import io.cattle.platform.core.model.Port;
import io.cattle.platform.core.model.StoragePool;
import io.cattle.platform.core.model.Volume;
import io.cattle.platform.core.model.VolumeStoragePoolMap;
import io.cattle.platform.core.model.tables.records.HostRecord;
import io.cattle.platform.core.model.tables.records.StoragePoolRecord;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.DataAccessor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.lang.StringUtils;
import org.jooq.Condition;
import org.jooq.Record3;
import org.jooq.RecordHandler;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AllocatorDaoImpl extends AbstractJooqDao implements AllocatorDao {

    private static final Logger log = LoggerFactory.getLogger(AllocatorDaoImpl.class);

    ObjectManager objectManager;
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
    public List<? extends Host> getHosts(Instance instance) {
        return create()
                .select(HOST.fields())
                .from(HOST)
                .join(INSTANCE_HOST_MAP)
                    .on(INSTANCE_HOST_MAP.HOST_ID.eq(HOST.ID))
                .where(
                    INSTANCE_HOST_MAP.REMOVED.isNull()
                    .and(INSTANCE_HOST_MAP.INSTANCE_ID.eq(instance.getId())))
                .fetchInto(HostRecord.class);
    }

    @Override
    public List<? extends Host> getHosts(StoragePool pool) {
        return create()
                .select(HOST.fields())
                .from(HOST)
                .join(STORAGE_POOL_HOST_MAP)
                    .on(STORAGE_POOL_HOST_MAP.HOST_ID.eq(HOST.ID))
                .where(STORAGE_POOL_HOST_MAP.REMOVED.isNull()
                    .and(STORAGE_POOL_HOST_MAP.STORAGE_POOL_ID.eq(pool.getId())))
                .fetchInto(HostRecord.class);
    }

    protected void modifyCompute(long hostId, Instance instance, boolean add) {
        Host host = objectManager.loadResource(Host.class, hostId);
        Long computeFree = host.getComputeFree();

        if ( computeFree == null ) {
            return;
        }

        long delta = AllocatorUtils.getCompute(instance);
        long newValue;
        if ( add ) {
            newValue = computeFree + delta;
        } else {
            newValue = computeFree - delta;
        }

        log.debug("Modifying computeFree on host [{}], {} {} {} = {}", host.getId(), computeFree,
                add ? "+" : "-", delta, newValue);

        host.setComputeFree(newValue);
        objectManager.persist(host);
    }

    @Override
    public boolean recordCandidate(AllocationAttempt attempt, AllocationCandidate candidate) {
        Set<Long> existingHosts = attempt.getHostIds();
        Set<Long> newHosts = candidate.getHosts();

        if ( existingHosts.size() == 0 ) {
            for ( long hostId : newHosts ) {
                log.info("Associating instance [{}] to host [{}]", attempt.getInstance().getId(), hostId);
                objectManager.create(InstanceHostMap.class,
                        INSTANCE_HOST_MAP.HOST_ID, hostId,
                        INSTANCE_HOST_MAP.INSTANCE_ID, attempt.getInstance().getId());

                modifyCompute(hostId, attempt.getInstance(), false);
            }
        } else {
            if ( ! existingHosts.equals(newHosts) ) {
                log.error("Can not move allocated instance [{}], currently {} new {}", attempt.getInstance().getId(),
                        existingHosts, newHosts);
                throw new IllegalStateException("Can not move allocated instance [" + attempt.getInstance().getId()
                        + "], currently " + existingHosts + " new " + newHosts);
            }
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

        for ( Nic nic : attempt.getNics() ) {
            Long subnetId = candidate.getSubnetIds().get(nic.getId());

            if ( subnetId == null || ( nic.getSubnetId() != null && subnetId.longValue() == nic.getSubnetId() ) ) {
                continue;
            }

            int i = create()
                .update(NIC)
                .set(NIC.SUBNET_ID, subnetId)
                .where(NIC.ID.eq(nic.getId()))
                .execute();

            if ( i != 1 ) {
                throw new IllegalStateException("Expected to update nic id ["
                            + nic.getId() + "] with subnet [" + subnetId + "] but update [" + i + "] rows");
            }
        }

        return true;
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
    public void releaseAllocation(Instance instance) {
        for ( InstanceHostMap map : mapDao.findNonRemoved(InstanceHostMap.class, Instance.class, instance.getId()) ) {
            DataAccessor data = DataAccessor.fromDataFieldOf(map)
                                    .withScope(AllocatorDaoImpl.class)
                                    .withKey("deallocated");

            Boolean done = data.as(Boolean.class);
            if ( done == null || ! done.booleanValue() ) {
                modifyCompute(map.getHostId(), instance, true);

                data.set(true);
                objectManager.persist(map);
            }
        }
    }

    @Override
    public void releaseAllocation(Volume volume) {
        // Nothing to do?
    }

    @Override
    public List<Long> getHostsForSubnet(long subnetId, Long vnetId) {
        return create()
            .select(HOST_VNET_MAP.HOST_ID)
            .from(SUBNET_VNET_MAP)
            .join(VNET)
                .on(VNET.ID.eq(SUBNET_VNET_MAP.VNET_ID))
            .join(HOST_VNET_MAP)
                .on(HOST_VNET_MAP.VNET_ID.eq(VNET.ID))
            .where(SUBNET_VNET_MAP.SUBNET_ID.eq(subnetId)
                    .and(vnetCondition(vnetId))
                    .and(HOST_VNET_MAP.STATE.eq(CommonStatesConstants.ACTIVE)))
            .fetch(HOST_VNET_MAP.HOST_ID);
    }

    @Override
    public List<Long> getPhysicalHostsForSubnet(long subnetId, Long vnetId) {
        return create()
            .select(HOST.PHYSICAL_HOST_ID)
            .from(SUBNET_VNET_MAP)
            .join(VNET)
                .on(VNET.ID.eq(SUBNET_VNET_MAP.VNET_ID))
            .join(HOST_VNET_MAP)
                .on(HOST_VNET_MAP.VNET_ID.eq(VNET.ID))
            .join(HOST)
                .on(HOST.ID.eq(HOST_VNET_MAP.HOST_ID))
            .where(SUBNET_VNET_MAP.SUBNET_ID.eq(subnetId)
                    .and(HOST_VNET_MAP.STATE.eq(CommonStatesConstants.ACTIVE))
                    .and(vnetCondition(vnetId))
                    .and(HOST.PHYSICAL_HOST_ID.isNotNull()))
            .fetchInto(Long.class);
    }

    protected Condition vnetCondition(Long vnetId) {
        if ( vnetId == null ) {
            return DSL.trueCondition();
        }

        return VNET.ID.eq(vnetId);
    }

    public ObjectManager getObjectManager() {
        return objectManager;
    }

    @Inject
    public void setObjectManager(ObjectManager objectManager) {
        this.objectManager = objectManager;
    }

    public GenericMapDao getMapDao() {
        return mapDao;
    }

    @Inject
    public void setMapDao(GenericMapDao mapDao) {
        this.mapDao = mapDao;
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
        final Map<String, String[]> labelKeyValueStatusMap = new HashMap<String, String[]>();

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
    public List<? extends Host> getNonPurgedHosts(long accountId) {
        return create()
                .select(HOST.fields())
                .from(HOST)
                .where(HOST.ACCOUNT_ID.eq(accountId)
                .and(HOST.STATE.notEqual(CommonStatesConstants.PURGED)))
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
                    AGENT.ID.isNull().or(AGENT.STATE.eq(CommonStatesConstants.ACTIVE))
                .and(HOST.REMOVED.isNull())
                .and(HOST.ACCOUNT_ID.eq(accountId))
                .and(HOST.STATE.in(CommonStatesConstants.ACTIVE, CommonStatesConstants.UPDATING_ACTIVE)))
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
}
