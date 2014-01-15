package io.github.ibuildthecloud.dstack.core.dao.impl;

import static io.github.ibuildthecloud.dstack.core.model.tables.ImageStoragePoolMapTable.*;

import io.github.ibuildthecloud.dstack.core.dao.ImageStoragePoolMapDao;
import io.github.ibuildthecloud.dstack.core.model.ImageStoragePoolMap;

public class ImageStoragePoolMapDaoImpl extends AbstractCoreDao implements ImageStoragePoolMapDao {

    @Override
    public ImageStoragePoolMap findNonRemovedMap(long imageId, long storagePoolId) {
        return create().selectFrom(IMAGE_STORAGE_POOL_MAP)
                .where(
                        IMAGE_STORAGE_POOL_MAP.REMOVED.isNull()
                        .and(IMAGE_STORAGE_POOL_MAP.IMAGE_ID.eq(imageId))
                        .and(IMAGE_STORAGE_POOL_MAP.STORAGE_POOL_ID.eq(storagePoolId)))
                .fetchOne();
    }

}
