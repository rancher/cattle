package io.cattle.platform.docker.process.lock;

import io.cattle.platform.core.model.StoragePool;
import io.cattle.platform.lock.definition.AbstractBlockingLockDefintion;

public class DockerStoragePoolVolumeCreateLock extends AbstractBlockingLockDefintion {

    public DockerStoragePoolVolumeCreateLock(StoragePool storagePool, String externalId) {
        super(String.format("DOCKER.STORAGE_POOL.VOLUME.CREATE.%s.%s", storagePool.getId(), externalId == null ? 0 : externalId.hashCode()));
    }
}
