package io.cattle.platform.storage.pool;

import io.cattle.platform.core.model.Image;
import io.cattle.platform.core.model.StoragePool;

import java.io.IOException;

public interface StoragePoolDriver {

    boolean supportsPool(StoragePool pool);

    boolean populateExtenalImage(StoragePool pool, String uuid, Image image) throws IOException;

}
