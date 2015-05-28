package io.cattle.platform.storage.pool;

import io.cattle.platform.core.model.Image;

public interface StoragePoolDriver {

    boolean populateImage(String uuid, Image image);

}
