package io.cattle.platform.storage.service;

import io.cattle.platform.core.model.Image;

public interface StorageService {

    Image registerRemoteImage(String uuid);

    boolean isValidUUID(String uuid);

}
