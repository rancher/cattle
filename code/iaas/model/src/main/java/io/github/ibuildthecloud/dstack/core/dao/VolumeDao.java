package io.github.ibuildthecloud.dstack.core.dao;

import io.github.ibuildthecloud.dstack.core.model.VolumeStoragePoolMap;

import java.util.List;

public interface VolumeDao {

    List<? extends VolumeStoragePoolMap> findNonRemovedVolumeStoragePoolMaps(long volumeId);

}
