package io.cattle.platform.docker.storage.dao;

import io.cattle.platform.core.model.StoragePool;

public interface DockerStorageDao {

    StoragePool getExternalStoragePool(StoragePool parentPool);

    StoragePool createExternalStoragePool(StoragePool parentPool);

}
