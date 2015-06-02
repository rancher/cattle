package io.cattle.platform.storage.service.impl;

import io.cattle.platform.core.dao.GenericResourceDao;
import io.cattle.platform.core.model.Image;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.storage.pool.StoragePoolDriver;
import io.cattle.platform.storage.service.StorageService;
import io.cattle.platform.storage.service.dao.ImageDao;

import java.util.List;

import javax.inject.Inject;

public class StorageServiceImpl implements StorageService {

    @Inject
    ObjectManager objectManager;
    List<StoragePoolDriver> drivers;
    @Inject
    GenericResourceDao genericResourceDao;
    @Inject
    ImageDao imageDao;

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

}
