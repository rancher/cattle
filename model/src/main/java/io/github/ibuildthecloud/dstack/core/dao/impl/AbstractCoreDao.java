package io.github.ibuildthecloud.dstack.core.dao.impl;

import javax.inject.Inject;

import io.github.ibuildthecloud.dstack.db.jooq.dao.impl.AbstractJooqDao;
import io.github.ibuildthecloud.dstack.object.ObjectManager;

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
