package io.cattle.platform.core.dao.impl;

import io.cattle.platform.lock.definition.AbstractBlockingLockDefintion;

public class ServiceCreateLock extends AbstractBlockingLockDefintion {

    public ServiceCreateLock(long stackId, String serviceName) {
        super("SERVICE.CREATE." + stackId + "." + serviceName);
    }
}
