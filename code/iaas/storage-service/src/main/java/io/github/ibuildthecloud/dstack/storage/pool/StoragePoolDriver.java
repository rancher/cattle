package io.github.ibuildthecloud.dstack.storage.pool;

import java.io.IOException;

import io.github.ibuildthecloud.dstack.core.model.Image;
import io.github.ibuildthecloud.dstack.core.model.StoragePool;

public interface StoragePoolDriver {

    boolean supportsPool(StoragePool pool);

    boolean populateExtenalImage(StoragePool pool, String uuid, Image image) throws IOException;

}
