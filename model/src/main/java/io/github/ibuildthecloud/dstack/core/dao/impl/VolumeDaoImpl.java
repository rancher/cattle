package io.github.ibuildthecloud.dstack.core.dao.impl;

import java.util.List;

import static io.github.ibuildthecloud.dstack.core.model.tables.VolumeStoragePoolMapTable.*;

import io.github.ibuildthecloud.dstack.core.dao.VolumeDao;
import io.github.ibuildthecloud.dstack.core.model.VolumeStoragePoolMap;

public class VolumeDaoImpl extends AbstractCoreDao implements VolumeDao {

    @Override
    public List<? extends VolumeStoragePoolMap> findNonRemovedVolumeStoragePoolMaps(long volumeId) {
        return create()
                .selectFrom(VOLUME_STORAGE_POOL_MAP)
                .where(
                        VOLUME_STORAGE_POOL_MAP.VOLUME_ID.eq(volumeId)
                        .and(VOLUME_STORAGE_POOL_MAP.REMOVED.isNull()))
                .fetch();
    }

}
