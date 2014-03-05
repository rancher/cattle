package io.github.ibuildthecloud.dstack.docker.storage.dao;

import io.github.ibuildthecloud.dstack.core.model.StoragePool;

public interface DockerStorageDao {

    StoragePool getExternalStoragePool();

    StoragePool createExternalStoragePool();

}
