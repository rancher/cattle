package io.github.ibuildthecloud.dstack.core.dao;

import io.github.ibuildthecloud.dstack.core.model.ImageStoragePoolMap;

public interface ImageStoragePoolMapDao {

    ImageStoragePoolMap findNonRemovedMap(long imageId, long storagePoolId);

}
