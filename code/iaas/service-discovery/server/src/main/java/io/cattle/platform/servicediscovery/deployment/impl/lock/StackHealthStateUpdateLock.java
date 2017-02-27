package io.cattle.platform.servicediscovery.deployment.impl.lock;

import io.cattle.platform.lock.definition.AbstractLockDefinition;

public class StackHealthStateUpdateLock extends AbstractLockDefinition {

    public StackHealthStateUpdateLock(long stackId) {
        super("STACK." + stackId + ".HEALTHSTATE.UPDATE");
    }

}
