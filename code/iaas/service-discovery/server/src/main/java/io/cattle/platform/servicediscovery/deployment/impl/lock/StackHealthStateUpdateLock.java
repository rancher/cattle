package io.cattle.platform.servicediscovery.deployment.impl.lock;

import io.cattle.platform.lock.definition.AbstractBlockingLockDefintion;

public class StackHealthStateUpdateLock extends AbstractBlockingLockDefintion {

    public StackHealthStateUpdateLock(long stackId) {
        super("STACK." + stackId + "HEALTHSTATE.UPDATE");
    }

    @Override
    public long getWait() {
        return super.getWait() * 2;
    }
}
