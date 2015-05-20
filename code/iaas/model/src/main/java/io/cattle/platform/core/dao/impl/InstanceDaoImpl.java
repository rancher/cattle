package io.cattle.platform.core.dao.impl;

import static io.cattle.platform.core.model.tables.InstanceHostMapTable.*;
import static io.cattle.platform.core.model.tables.InstanceTable.*;
import static io.cattle.platform.core.model.tables.StoragePoolHostMapTable.*;
import static io.cattle.platform.core.model.tables.StoragePoolTable.*;
import static io.cattle.platform.core.model.tables.VolumeStoragePoolMapTable.*;
import static io.cattle.platform.core.model.tables.VolumeTable.*;
import io.cattle.platform.core.dao.InstanceDao;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.tables.records.InstanceRecord;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.jooq.Condition;

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

        if (poolIds.size() == 0) {
            return false;
        }

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

    @Override
    public List<? extends Instance> getNonRemovedInstanceOn(Long hostId) {
        return create()
                .select(INSTANCE.fields())
                .from(INSTANCE)
                .join(INSTANCE_HOST_MAP)
                    .on(INSTANCE_HOST_MAP.HOST_ID.eq(hostId)
                            .and(INSTANCE_HOST_MAP.INSTANCE_ID.eq(INSTANCE.ID)))
                .where(INSTANCE.REMOVED.isNull())
                .fetchInto(InstanceRecord.class);
    }

    @Override
    public Instance getInstanceByUuidOrExternalId(Long accountId, String uuid, String externalId) {
        Instance instance = null;
        Condition accountCondition = INSTANCE.ACCOUNT_ID.eq(accountId).and(INSTANCE.REMOVED.isNull());

        if(StringUtils.isNotEmpty(uuid)) {
            instance = create()
                    .selectFrom(INSTANCE)
                    .where(accountCondition
                    .and(INSTANCE.UUID.eq(uuid)))
                    .fetchAny();
        }

        if (instance == null && StringUtils.isNotEmpty(externalId)) {
            instance = create()
                    .selectFrom(INSTANCE)
                    .where(accountCondition
                    .and(INSTANCE.EXTERNAL_ID.eq(externalId)))
                    .fetchAny();
        }

        return instance;
    }
}
