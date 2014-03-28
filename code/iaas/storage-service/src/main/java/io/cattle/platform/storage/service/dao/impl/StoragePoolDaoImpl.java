package io.cattle.platform.storage.service.dao.impl;

import static io.cattle.platform.core.model.tables.StoragePoolTable.*;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.model.StoragePool;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.storage.service.dao.StoragePoolDao;

import java.util.List;

public class StoragePoolDaoImpl extends AbstractJooqDao implements StoragePoolDao {

    @Override
    public List<? extends StoragePool> findExternalActivePools() {
        return create()
                .selectFrom(STORAGE_POOL)
                .where(
                    STORAGE_POOL.EXTERNAL.eq(true)
                    .and(STORAGE_POOL.STATE.eq(CommonStatesConstants.ACTIVE))
                ).fetch();
    }

}
