package io.github.ibuildthecloud.dstack.storage.service;

import java.io.IOException;

import io.github.ibuildthecloud.dstack.core.model.Image;

public interface StorageService {

    Image registerRemoteImage(String uuid) throws IOException;

}
