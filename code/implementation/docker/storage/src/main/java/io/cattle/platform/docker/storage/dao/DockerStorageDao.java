package io.cattle.platform.docker.storage.dao;

import io.cattle.platform.core.model.Image;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.StoragePool;

public interface DockerStorageDao {

    StoragePool getExternalStoragePool(StoragePool parentPool);

    StoragePool createExternalStoragePool(StoragePool parentPool);

    Image createImageForInstance(Instance instance);

}
