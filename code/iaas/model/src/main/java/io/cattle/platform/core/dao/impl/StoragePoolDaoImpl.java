package io.cattle.platform.core.dao.impl;

import static io.cattle.platform.core.model.tables.StoragePoolTable.*;
import static io.cattle.platform.core.model.tables.StoragePoolHostMapTable.*;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.dao.GenericResourceDao;
import io.cattle.platform.core.dao.StoragePoolDao;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.StoragePool;
import io.cattle.platform.core.model.StoragePoolHostMap;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

public class StoragePoolDaoImpl extends AbstractJooqDao implements StoragePoolDao {

    GenericResourceDao resourceDao;

    @Override
    public List<? extends StoragePool> findExternalActivePools() {
        return create()
                .selectFrom(STORAGE_POOL)
                .where(
                        STORAGE_POOL.EXTERNAL.eq(true)
                        .and(STORAGE_POOL.STATE.eq(CommonStatesConstants.ACTIVE))
                        ).fetch();
    }

    @Override
    public StoragePool mapNewPool(Host host, Map<String, Object> properties) {
        StoragePool pool = resourceDao.createAndSchedule(StoragePool.class, properties);
        resourceDao.createAndSchedule(StoragePoolHostMap.class,
                STORAGE_POOL_HOST_MAP.HOST_ID, host.getId(),
                STORAGE_POOL_HOST_MAP.STORAGE_POOL_ID, pool.getId());

        return pool;
    }

    public GenericResourceDao getResourceDao() {
        return resourceDao;
    }

    @Inject
    public void setResourceDao(GenericResourceDao resourceDao) {
        this.resourceDao = resourceDao;
    }

}
