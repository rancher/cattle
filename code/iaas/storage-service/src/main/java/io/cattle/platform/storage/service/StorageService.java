package io.cattle.platform.storage.service;

import io.cattle.platform.core.model.Image;
import io.cattle.platform.core.model.StorageDriver;

public interface StorageService {

    Image registerRemoteImage(String uuid);

    boolean isValidUUID(String uuid);

    void setupPools(StorageDriver storageDriver);

}