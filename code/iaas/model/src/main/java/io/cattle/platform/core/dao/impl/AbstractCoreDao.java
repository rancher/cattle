package io.cattle.platform.core.dao.impl;

import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.object.ObjectManager;

import org.jooq.Configuration;

public abstract class AbstractCoreDao extends AbstractJooqDao {

    ObjectManager objectManager;

    public AbstractCoreDao(Configuration configuration, ObjectManager objectManager) {
        super(configuration);
        this.objectManager = objectManager;
    }

}
