package io.cattle.platform.core.dao.impl;

import static io.cattle.platform.core.model.tables.StoragePoolHostMapTable.*;
import static io.cattle.platform.core.model.tables.StoragePoolTable.*;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.dao.GenericResourceDao;
import io.cattle.platform.core.dao.StoragePoolDao;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.StoragePool;
import io.cattle.platform.core.model.StoragePoolHostMap;
import io.cattle.platform.core.model.tables.records.StoragePoolRecord;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

public class StoragePoolDaoImpl extends AbstractJooqDao implements StoragePoolDao {

    @Inject
    GenericResourceDao resourceDao;

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
        StoragePool pool = resourceDao.createAndSchedule(StoragePool.class, properties);
        resourceDao.createAndSchedule(StoragePoolHostMap.class,
                STORAGE_POOL_HOST_MAP.HOST_ID, host.getId(),
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
}
