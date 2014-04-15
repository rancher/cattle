package io.cattle.platform.storage.service.impl;

import io.cattle.platform.core.model.Image;
import io.cattle.platform.core.model.StoragePool;
import io.cattle.platform.lock.LockCallback;
import io.cattle.platform.lock.LockManager;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.storage.pool.StoragePoolDriver;
import io.cattle.platform.storage.service.StorageService;
import io.cattle.platform.storage.service.dao.ImageDao;
import io.cattle.platform.storage.service.dao.StoragePoolDao;
import io.cattle.platform.storage.service.lock.ExternalTemplateRegister;

import java.io.IOException;
import java.util.List;

import javax.inject.Inject;

public class StorageServiceImpl implements StorageService {

    ObjectManager objectManager;
    List<StoragePoolDriver> drivers;
    StoragePoolDao storagePoolDao;
    ImageDao imageDao;
    LockManager lockManager;

    @Override
    public Image registerRemoteImage(final String uuid) throws IOException {
        if ( uuid == null ) {
            return null;
        }

        Image existing = imageDao.findImageByUuid(uuid);
        if ( existing != null ) {
            return existing;
        }

        return populateNewRecord(uuid);
    }

    protected Image populateNewRecord(String uuid) throws IOException {
        Image image = objectManager.newRecord(Image.class);
        image.setUuid(uuid);
        image.setIsPublic(true);

        StoragePool foundPool = null;

        for ( StoragePool pool : storagePoolDao.findExternalActivePools() ) {
            for ( StoragePoolDriver driver : drivers ) {
                if ( driver.supportsPool(pool) ) {
                    if ( driver.populateExtenalImage(pool, uuid, image) ) {
                        foundPool = pool;
                        break;
                    }
                }
            }
        }

        if ( foundPool != null ) {
            return persistAndCreate(uuid, image, foundPool);
        }

        return null;
    }

    protected Image persistAndCreate(final String uuid, final Image image, final StoragePool pool) {
        return lockManager.lock(new ExternalTemplateRegister(uuid), new LockCallback<Image>() {
            @Override
            public Image doWithLock() {
                Image existing = imageDao.findImageByUuid(uuid);
                if ( existing != null ) {
                    return existing;
                }

                return imageDao.persistAndAssociateImage(image, pool);
            }
        });
    }

    public StoragePoolDao getStoragePoolDao() {
        return storagePoolDao;
    }

    @Inject
    public void setStoragePoolDao(StoragePoolDao storagePoolDao) {
        this.storagePoolDao = storagePoolDao;
    }

    public List<StoragePoolDriver> getDrivers() {
        return drivers;
    }

    @Inject
    public void setDrivers(List<StoragePoolDriver> drivers) {
        this.drivers = drivers;
    }

    public ImageDao getImageDao() {
        return imageDao;
    }

    @Inject
    public void setImageDao(ImageDao imageDao) {
        this.imageDao = imageDao;
    }

    public LockManager getLockManager() {
        return lockManager;
    }

    @Inject
    public void setLockManager(LockManager lockManager) {
        this.lockManager = lockManager;
    }

    public ObjectManager getObjectManager() {
        return objectManager;
    }

    @Inject
    public void setObjectManager(ObjectManager objectManager) {
        this.objectManager = objectManager;
    }

}