package io.cattle.platform.core.dao;

import io.cattle.platform.core.model.StoragePool;
import io.cattle.platform.core.model.Volume;

import java.util.Map;

public interface VolumeDao {
    Volume findVolumeByExternalId(Long storagePoolId, String externalId);
    
    void createVolumeInStoragePool(Map<String, Object> volumeData, StoragePool storagePool);

    /**
     * Does what the name says, but if storagePoolId is null, will look across all non-local storage pools
     * if storagePoolId is not null, will restrict the lookup to that storage pool.
     */
    Volume findSharedVolume(long accountId, Long storagePoolId, String volumeName);
}
