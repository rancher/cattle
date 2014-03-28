package io.cattle.platform.storage.service;

import java.io.IOException;

import io.cattle.platform.core.model.Image;

public interface StorageService {

    Image registerRemoteImage(String uuid) throws IOException;

}
