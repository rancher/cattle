package io.cattle.platform.core.dao.impl;

import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.object.ObjectManager;

import javax.inject.Inject;

public abstract class AbstractCoreDao extends AbstractJooqDao {

    protected ObjectManager objectManager;

    public ObjectManager getObjectManager() {
        return objectManager;
    }

    @Inject
    public void setObjectManager(ObjectManager objectManager) {
        this.objectManager = objectManager;
    }

}
