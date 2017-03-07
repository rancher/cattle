package io.cattle.platform.servicediscovery.api.lock;

import io.cattle.platform.lock.definition.AbstractBlockingLockDefintion;

public class CreateServiceLock extends AbstractBlockingLockDefintion {
    public CreateServiceLock(long stackId) {
        super("SERVICE.CREATE.FOR.STACK." + stackId);
    }
}
