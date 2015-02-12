package io.cattle.platform.storage.service;

import io.cattle.platform.core.model.Image;

import java.io.IOException;

public interface StorageService {

    Image registerRemoteImage(String uuid) throws IOException;

}
