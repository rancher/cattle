package io.cattle.platform.allocator.dao.impl;

import static io.cattle.platform.core.model.tables.HostTable.*;
import static io.cattle.platform.core.model.tables.ImageStoragePoolMapTable.*;
import static io.cattle.platform.core.model.tables.ImageTable.*;
import static io.cattle.platform.core.model.tables.InstanceHostMapTable.*;
import static io.cattle.platform.core.model.tables.InstanceTable.*;
import static io.cattle.platform.core.model.tables.NicTable.*;
import static io.cattle.platform.core.model.tables.StoragePoolHostMapTable.*;
import static io.cattle.platform.core.model.tables.StoragePoolTable.*;
import static io.cattle.platform.core.model.tables.VolumeStoragePoolMapTable.*;
import io.cattle.platform.allocator.dao.AllocatorDao;
import io.cattle.platform.allocator.service.AllocationAttempt;
import io.cattle.platform.allocator.service.AllocationCandidate;
import io.cattle.platform.allocator.util.AllocatorUtils;
import io.cattle.platform.core.dao.GenericMapDao;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.InstanceHostMap;
import io.cattle.platform.core.model.Nic;
import io.cattle.platform.core.model.StoragePool;
import io.cattle.platform.core.model.Volume;
import io.cattle.platform.core.model.VolumeStoragePoolMap;
import io.cattle.platform.core.model.tables.records.HostRecord;
import io.cattle.platform.core.model.tables.records.StoragePoolRecord;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.DataAccessor;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

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
    public List<? extends StoragePool> getAssociatedPools(Host host) {
        return create()
                .select(STORAGE_POOL.fields())
                .from(STORAGE_POOL)
                .join(STORAGE_POOL_HOST_MAP)
                    .on(STORAGE_POOL_HOST_MAP.STORAGE_POOL_ID.eq(STORAGE_POOL.ID))
                .where(
                    STORAGE_POOL_HOST_MAP.REMOVED.isNull()
                    .and(STORAGE_POOL_HOST_MAP.HOST_ID.eq(host.getId())))
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

        Map<Long,Set<Long>> existingPools = attempt.getPoolIds();
        Map<Long,Set<Long>> newPools = candidate.getPools();

        if ( isEmtpy(existingPools) ) {
            for ( Map.Entry<Long, Set<Long>> entry : newPools.entrySet() ) {
                long volumeId = entry.getKey();
                for ( long poolId : entry.getValue() ) {
                    boolean inRightState = true;
//                    for ( Volume v : attempt.getVolumes() ) {
//                        if ( v.getId().longValue() == volumeId ) {
//                            Boolean stateCheck = AllocatorUtils.checkAllocateState(v.getId(), v.getAllocationState(), "Volume");
//                            if ( stateCheck != null && ! stateCheck.booleanValue() ) {
//                                log.error("Not assigning volume [{}] to pool [{}] because it is in state [{}]",
//                                        v.getId(), poolId, v.getAllocationState());
//                                inRightState = false;
//                            }
//                        }
//                    }
                    if ( inRightState ) {
                        log.info("Associating volume [{}] to storage pool [{}]", volumeId, poolId);
                        objectManager.create(VolumeStoragePoolMap.class,
                                VOLUME_STORAGE_POOL_MAP.VOLUME_ID, volumeId,
                                VOLUME_STORAGE_POOL_MAP.STORAGE_POOL_ID, poolId);
                    }
                }
            }
        } else {
            if ( ! existingPools.equals(newPools) ) {
                log.error("Can not move volumes, currently {} new {}", existingPools, newPools);
                throw new IllegalStateException("Can not move volumes, currently " + existingPools + " new " + newPools);
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

}