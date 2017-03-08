package io.cattle.platform.core.dao.impl;

import io.cattle.platform.lock.definition.AbstractBlockingLockDefintion;

public class DefaultDeploymentUnitCreateLock extends AbstractBlockingLockDefintion {

    public DefaultDeploymentUnitCreateLock(long serviceId) {
        super("DU.CREATE." + serviceId);
    }
}
