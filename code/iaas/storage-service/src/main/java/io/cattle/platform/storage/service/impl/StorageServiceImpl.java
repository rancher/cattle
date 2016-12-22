package io.cattle.platform.storage.service.impl;

import static io.cattle.platform.core.model.tables.StoragePoolHostMapTable.*;
import static io.cattle.platform.core.model.tables.StoragePoolTable.*;

import io.cattle.platform.core.constants.StorageDriverConstants;
import io.cattle.platform.core.constants.StoragePoolConstants;
import io.cattle.platform.core.dao.GenericResourceDao;
import io.cattle.platform.core.dao.StoragePoolDao;
import io.cattle.platform.core.model.Image;
import io.cattle.platform.core.model.StorageDriver;
import io.cattle.platform.core.model.StoragePool;
import io.cattle.platform.core.model.StoragePoolHostMap;
import io.cattle.platform.lock.LockCallbackNoReturn;
import io.cattle.platform.lock.LockManager;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.storage.pool.StoragePoolDriver;
import io.cattle.platform.storage.service.StorageService;
import io.cattle.platform.storage.service.dao.ImageDao;
import io.cattle.platform.util.type.CollectionUtils;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

public class StorageServiceImpl implements StorageService {

    @Inject
    ObjectManager objectManager;
    List<StoragePoolDriver> drivers;
    @Inject
    GenericResourceDao genericResourceDao;
    @Inject
    ImageDao imageDao;
    @Inject
    LockManager lockManager;
    @Inject
    StoragePoolDao storagePoolDao;


    @Override
    public Image registerRemoteImage(final String uuid) {
        if (uuid == null) {
            return null;
        }
        return populateNewRecord(uuid);
    }

    @Override
    public boolean isValidUUID(String uuid) {
        Image image = objectManager.newRecord(Image.class);
        for (StoragePoolDriver driver : drivers) {
            if (driver.populateImage(uuid, image)) {
                return true;
            }
        }
        return false;
    }

    protected Image populateNewRecord(String uuid) {
        Image image = objectManager.newRecord(Image.class);
        for (StoragePoolDriver driver : drivers) {
            if (driver.populateImage(uuid, image)) {
                break;
            }
        }
        return genericResourceDao.createAndSchedule(image);
    }

    public List<StoragePoolDriver> getDrivers() {
        return drivers;
    }

    @Inject
    public void setDrivers(List<StoragePoolDriver> drivers) {
        this.drivers = drivers;
    }

    @Override
    public void setupPools(final StorageDriver storageDriver) {
        String scope = DataAccessor.fieldString(storageDriver, StorageDriverConstants.FIELD_SCOPE);
        if (StorageDriverConstants.SCOPE_CUSTOM.equals(scope)) {
            return;
        }

        lockManager.lock(new StorageDriverLock(storageDriver), new LockCallbackNoReturn() {
            @Override
            public void doWithLockNoResult() {
                setupPoolsWithLock(storageDriver);
            }
        });
    }

    protected Map<String, Object> getStoragePoolProperties(StorageDriver storageDriver) {
        return objectManager.convertToPropertiesFor(StoragePool.class, CollectionUtils.asMap((Object)
                STORAGE_POOL.ACCOUNT_ID, storageDriver.getAccountId(),
                STORAGE_POOL.NAME, storageDriver.getName(),
                STORAGE_POOL.DRIVER_NAME, storageDriver.getName(),
                STORAGE_POOL.STORAGE_DRIVER_ID, storageDriver.getId(),
                STORAGE_POOL.VOLUME_ACCESS_MODE,
                    DataAccessor.fieldString(storageDriver, StorageDriverConstants.FIELD_VOLUME_ACCESS_MODE),
                StoragePoolConstants.FIELD_VOLUME_CAPABILITIES,
                    DataAccessor.fieldString(storageDriver, StoragePoolConstants.FIELD_VOLUME_CAPABILITIES)));
    }

    protected void setupPoolsWithLock(StorageDriver storageDriver) {
        boolean localScope = StorageDriverConstants.SCOPE_LOCAL.equals(DataAccessor.fieldString(storageDriver, StorageDriverConstants.FIELD_SCOPE));
        StoragePool globalPool = objectManager.findAny(StoragePool.class,
                STORAGE_POOL.STORAGE_DRIVER_ID, storageDriver.getId(),
                STORAGE_POOL.REMOVED, null);
        Map<Long, Long> hostToPool = storagePoolDao.findStoragePoolHostsByDriver(storageDriver.getAccountId(),
                storageDriver.getId());
        for (Map.Entry<Long, Long> entry : hostToPool.entrySet()) {
            Long hostId = entry.getKey();
            Long storagePoolId = entry.getValue();
            if (storagePoolId != null) {
                continue;
            }

            if (localScope) {
                storagePoolDao.mapNewPool(hostId, getStoragePoolProperties(storageDriver));
            } else {
                if (globalPool == null) {
                    globalPool = storagePoolDao.mapNewPool(hostId, getStoragePoolProperties(storageDriver));
                } else {
                    genericResourceDao.createAndSchedule(StoragePoolHostMap.class,
                            STORAGE_POOL_HOST_MAP.HOST_ID, hostId,
                            STORAGE_POOL_HOST_MAP.STORAGE_POOL_ID, globalPool.getId());
                }
            }
        }
    }
}