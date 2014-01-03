package io.github.ibuildthecloud.dstack.storage.service.impl;

import io.github.ibuildthecloud.dstack.core.model.Image;
import io.github.ibuildthecloud.dstack.core.model.StoragePool;
import io.github.ibuildthecloud.dstack.core.model.tables.records.ImageRecord;
import io.github.ibuildthecloud.dstack.storage.pool.StoragePoolDriver;
import io.github.ibuildthecloud.dstack.storage.service.StorageService;
import io.github.ibuildthecloud.dstack.storage.service.dao.ImageDao;
import io.github.ibuildthecloud.dstack.storage.service.dao.StoragePoolDao;

import java.io.IOException;
import java.util.List;

import javax.inject.Inject;

public class StorageServiceImpl implements StorageService {

    List<StoragePoolDriver> drivers;
    StoragePoolDao storagePoolDao;
    ImageDao imageDao;

    @Override
    public Image registerRemoteImage(String uuid) throws IOException {
        Image existing = imageDao.findImageByUuid(uuid);
        if ( existing != null ) {
            return existing;
        }

        return populateNewRecord(uuid);
    }

    protected Image populateNewRecord(String uuid) throws IOException {
        ImageRecord imageRecord = new ImageRecord();
        imageRecord.setUuid(uuid);
        imageRecord.setIsPublic(true);

        StoragePool foundPool = null;

        for ( StoragePool pool : storagePoolDao.findExternalActivePools() ) {
            for ( StoragePoolDriver driver : drivers ) {
                if ( driver.supportsPool(pool) ) {
                    if ( driver.populateExtenalImage(pool, uuid, imageRecord) ) {
                        foundPool = pool;
                        break;
                    }
                }
            }
        }

        if ( foundPool != null ) {
            return imageDao.persistAndAssociateImage(imageRecord, foundPool);
        }

        return null;
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

}