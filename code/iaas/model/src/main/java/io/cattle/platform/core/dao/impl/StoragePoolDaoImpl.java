package io.cattle.platform.core.dao.impl;

import static io.cattle.platform.core.model.tables.HostTable.*;
import static io.cattle.platform.core.model.tables.StorageDriverTable.*;
import static io.cattle.platform.core.model.tables.StoragePoolHostMapTable.*;
import static io.cattle.platform.core.model.tables.StoragePoolTable.*;
import static io.cattle.platform.core.model.tables.VolumeStoragePoolMapTable.*;
import static io.cattle.platform.core.model.tables.VolumeTable.*;

import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.dao.GenericMapDao;
import io.cattle.platform.core.dao.GenericResourceDao;
import io.cattle.platform.core.dao.StoragePoolDao;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.StoragePool;
import io.cattle.platform.core.model.StoragePoolHostMap;
import io.cattle.platform.core.model.Volume;
import io.cattle.platform.core.model.VolumeStoragePoolMap;
import io.cattle.platform.core.model.tables.records.StoragePoolRecord;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.jooq.Record;

public class StoragePoolDaoImpl extends AbstractJooqDao implements StoragePoolDao {

    @Inject
    GenericResourceDao resourceDao;

    @Inject
    GenericMapDao genericMapDao;

    @Inject
    ObjectManager objectManager;

    @Inject
    ObjectProcessManager objectProcessManager;

    @Override
    public List<? extends StoragePool> findExternalActivePools() {
        return create()
                .selectFrom(STORAGE_POOL)
                .where(
                        STORAGE_POOL.EXTERNAL.eq(true)
                        .and(STORAGE_POOL.STATE.eq(CommonStatesConstants.ACTIVE))
                        ).fetch();
    }

    @Override
    public StoragePool mapNewPool(Host host, Map<String, Object> properties) {
        return mapNewPool(host.getId(), properties);
    }

    @Override
    public StoragePool mapNewPool(Long hostId, Map<String, Object> properties) {
        StoragePool pool = resourceDao.createAndSchedule(StoragePool.class, properties);
        resourceDao.createAndSchedule(StoragePoolHostMap.class,
                STORAGE_POOL_HOST_MAP.HOST_ID, hostId,
                STORAGE_POOL_HOST_MAP.STORAGE_POOL_ID, pool.getId());

        return pool;
    }

    @Override
    public List<? extends StoragePool> findStoragePoolByDriverName(Long accountId, String driverName) {
        return create().selectFrom(STORAGE_POOL)
            .where(STORAGE_POOL.ACCOUNT_ID.eq(accountId))
            .and((STORAGE_POOL.REMOVED.isNull().or(STORAGE_POOL.STATE.eq(CommonStatesConstants.REMOVING))))
            .and(STORAGE_POOL.DRIVER_NAME.eq(driverName))
            .fetchInto(StoragePoolRecord.class);
    }

    @Override
    public List<? extends StoragePoolHostMap> findMapsToRemove(Long storagePoolId) {
        return create()
            .selectFrom(STORAGE_POOL_HOST_MAP)
            .where(STORAGE_POOL_HOST_MAP.STORAGE_POOL_ID.eq(storagePoolId)
            .and((STORAGE_POOL_HOST_MAP.REMOVED.isNull()
                    .or(STORAGE_POOL_HOST_MAP.STATE.eq(CommonStatesConstants.REMOVING)))))
            .fetchInto(StoragePoolHostMap.class);
    }

    @Override
    public StoragePoolHostMap findNonremovedMap(Long storagePoolId, Long hostId) {
        List<StoragePoolHostMap> maps = objectManager.find(StoragePoolHostMap.class,
                STORAGE_POOL_HOST_MAP.STORAGE_POOL_ID,
                storagePoolId, STORAGE_POOL_HOST_MAP.HOST_ID, hostId);
        for (StoragePoolHostMap map : maps) {
            if (map != null && (map.getRemoved() == null
                    || map.getState().equals(CommonStatesConstants.REMOVING))) {
                return map;
            }
        }
        return null;
    }

    @Override
    public void createStoragePoolHostMap(StoragePoolHostMap map) {
        StoragePoolHostMap found = findNonremovedMap(map.getStoragePoolId(), map.getHostId());
        if (found == null) {
            resourceDao.createAndSchedule(map);
        }
    }

    @Override
    public Map<Long, Long> findStoragePoolHostsByDriver(Long accountId, Long storageDriverId) {
        return create().select(HOST.ID, STORAGE_POOL.ID)
            .from(HOST)
            .leftOuterJoin(STORAGE_POOL_HOST_MAP)
                .on(STORAGE_POOL_HOST_MAP.HOST_ID.eq(HOST.ID))
            .leftOuterJoin(STORAGE_POOL)
                .on(STORAGE_POOL_HOST_MAP.STORAGE_POOL_ID.eq(STORAGE_POOL.ID)
                        .and(STORAGE_POOL.STORAGE_DRIVER_ID.eq(storageDriverId)))
            .where(STORAGE_POOL.REMOVED.isNull()
                .and(STORAGE_POOL_HOST_MAP.REMOVED.isNull())
                .and(HOST.ACCOUNT_ID.eq(accountId))
                .and(HOST.REMOVED.isNull()))
            .fetch().intoMap(HOST.ID, STORAGE_POOL.ID);
    }

    @Override
    public List<? extends StoragePool> findNonRemovedStoragePoolByDriver(Long storageDriverId) {
        return create().select(STORAGE_POOL.fields())
            .from(STORAGE_POOL)
            .where(STORAGE_POOL.REMOVED.isNull()
                    .and(STORAGE_POOL.STORAGE_DRIVER_ID.eq(storageDriverId)))
            .fetchInto(StoragePoolRecord.class);
    }

    @Override
    public List<Long> findVolumesInUseByServiceDriver(Long serviceId) {
        return create().select(VOLUME.ID)
            .from(VOLUME)
            .join(VOLUME_STORAGE_POOL_MAP)
                .on(VOLUME_STORAGE_POOL_MAP.VOLUME_ID.eq(VOLUME.ID))
            .join(STORAGE_POOL)
                .on(VOLUME_STORAGE_POOL_MAP.STORAGE_POOL_ID.eq(STORAGE_POOL.ID))
            .join(STORAGE_DRIVER)
                .on(STORAGE_DRIVER.ID.eq(STORAGE_POOL.STORAGE_DRIVER_ID))
            .where(STORAGE_DRIVER.REMOVED.isNull()
                    .and(STORAGE_DRIVER.SERVICE_ID.eq(serviceId))
                    .and(VOLUME.REMOVED.isNull()))
            .fetch().into(Long.class);
    }

    @Override
    public StoragePool associateVolumeToPool(Long volumeId, Long storageDriverId, Long hostId) {
        Record record = create().select(STORAGE_POOL.fields())
                .from(STORAGE_POOL)
                .join(STORAGE_POOL_HOST_MAP)
                    .on(STORAGE_POOL_HOST_MAP.STORAGE_POOL_ID.eq(STORAGE_POOL.ID))
                .where(STORAGE_POOL.STORAGE_DRIVER_ID.eq(storageDriverId)
                        .and(STORAGE_POOL_HOST_MAP.HOST_ID.eq(hostId)))
                .fetchAny();
        if (record == null) {
            return null;
        }
        StoragePool storagePool = record.into(StoragePoolRecord.class);
        VolumeStoragePoolMap map = genericMapDao.findNonRemoved(VolumeStoragePoolMap.class,
                StoragePool.class, storagePool.getId(), Volume.class, volumeId);
        if (map == null) {
            resourceDao.createAndSchedule(VolumeStoragePoolMap.class,
                    VOLUME_STORAGE_POOL_MAP.VOLUME_ID, volumeId,
                    VOLUME_STORAGE_POOL_MAP.STORAGE_POOL_ID, storagePool.getId());
        }

        return storagePool;
    }

}
