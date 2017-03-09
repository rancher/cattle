package io.cattle.platform.core.dao.impl;

import io.cattle.platform.lock.definition.AbstractBlockingLockDefintion;

public class ServiceCreateUpdateLock extends AbstractBlockingLockDefintion {

    public ServiceCreateUpdateLock(long stackId, String serviceName) {
        super("SERVICE.CREATE." + stackId + "." + serviceName);
    }
}
