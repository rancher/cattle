package io.cattle.platform.storage.service;

import io.cattle.platform.core.model.StorageDriver;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;

public interface StorageService {

    void validateImageAndSetImage(Object obj, boolean required) throws ClientVisibleException;

    void setupPools(StorageDriver storageDriver);

    String normalizeImageUuid(String image);

}