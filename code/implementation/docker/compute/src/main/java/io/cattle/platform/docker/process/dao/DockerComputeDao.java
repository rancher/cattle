package io.cattle.platform.docker.process.dao;

import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.IpAddress;
import io.cattle.platform.core.model.StoragePool;
import io.cattle.platform.core.model.Volume;

public interface DockerComputeDao {

    IpAddress getDockerIp(String ipAddress, Instance instance);

    Volume getDockerVolumeInPool(String volumeUri, StoragePool storagePool);

    Volume createDockerVolumeInPool(Long accountId, String volumeUri, StoragePool storagePool, boolean isHostPath);
}
