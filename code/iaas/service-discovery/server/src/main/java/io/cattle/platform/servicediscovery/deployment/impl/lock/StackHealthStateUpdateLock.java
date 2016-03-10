package io.cattle.platform.servicediscovery.deployment.impl.lock;

import io.cattle.platform.core.model.Environment;
import io.cattle.platform.lock.definition.AbstractBlockingLockDefintion;

public class StackHealthStateUpdateLock extends AbstractBlockingLockDefintion {

    public StackHealthStateUpdateLock(Environment stack) {
        super("STACK." + stack.getId() + "HEALTHSTATE.UPDATE");
    }

    @Override
    public long getWait() {
        return super.getWait() * 2;
    }
}
