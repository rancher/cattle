package io.github.ibuildthecloud.dstack.storage.service.dao.impl;

import static io.github.ibuildthecloud.dstack.core.model.tables.StoragePoolTable.*;
import io.github.ibuildthecloud.dstack.core.constants.CommonStatesConstants;
import io.github.ibuildthecloud.dstack.core.model.StoragePool;
import io.github.ibuildthecloud.dstack.db.jooq.dao.impl.AbstractJooqDao;
import io.github.ibuildthecloud.dstack.storage.service.dao.StoragePoolDao;

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
