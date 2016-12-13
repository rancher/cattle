package io.cattle.platform.docker.process.dao;

import io.cattle.platform.core.model.StoragePool;
import io.cattle.platform.core.model.Volume;

public interface DockerComputeDao {

    Volume getDockerVolumeInPool(String volumeUri, String externalId, StoragePool storagePool);

    Volume createDockerVolumeInPool(Long accountId, String name, String volumeUri, String externalId, String driver, StoragePool storagePool,
            boolean isHostPath, boolean isNative);
}
