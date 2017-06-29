package io.cattle.platform.storage.service;

import io.cattle.platform.core.model.StorageDriver;

public interface StorageService {

    boolean isValidUUID(String uuid);

    void setupPools(StorageDriver storageDriver);

}