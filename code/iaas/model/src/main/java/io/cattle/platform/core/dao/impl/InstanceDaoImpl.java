package io.cattle.platform.core.dao.impl;

import static io.cattle.platform.core.model.tables.StoragePoolHostMapTable.*;
import static io.cattle.platform.core.model.tables.StoragePoolTable.*;
import static io.cattle.platform.core.model.tables.VolumeStoragePoolMapTable.*;
import static io.cattle.platform.core.model.tables.VolumeTable.*;
import io.cattle.platform.core.dao.InstanceDao;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;

import java.util.List;

public class InstanceDaoImpl extends AbstractJooqDao implements InstanceDao {

    @Override
    public boolean isOnSharedStorage(Instance instance) {
        List<Long> poolIds = create()
                .select(STORAGE_POOL.ID)
                .from(STORAGE_POOL)
                .join(VOLUME_STORAGE_POOL_MAP)
                    .on(VOLUME_STORAGE_POOL_MAP.STORAGE_POOL_ID.eq(STORAGE_POOL.ID))
                .join(VOLUME)
                    .on(VOLUME_STORAGE_POOL_MAP.VOLUME_ID.eq(VOLUME.ID))
                .where(VOLUME.INSTANCE_ID.eq(instance.getId())
                        .and(VOLUME_STORAGE_POOL_MAP.REMOVED.isNull())
                        .and(VOLUME.REMOVED.isNull()))
                .fetchInto(Long.class);

        for ( Long poolId : poolIds ) {
            List<?> result = create()
                    .select()
                    .from(STORAGE_POOL_HOST_MAP)
                    .where(STORAGE_POOL_HOST_MAP.STORAGE_POOL_ID.eq(poolId)
                            .and(STORAGE_POOL_HOST_MAP.REMOVED.isNull()))
                    .limit(2)
                    .fetch();

            if ( result.size() <= 1 ) {
                return false;
            }
         }

        return true;
    }

}
