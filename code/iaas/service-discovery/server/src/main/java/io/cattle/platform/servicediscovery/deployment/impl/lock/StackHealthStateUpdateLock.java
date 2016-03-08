package io.cattle.platform.servicediscovery.deployment.impl.lock;

import io.cattle.platform.core.model.Environment;
import io.cattle.platform.lock.definition.AbstractLockDefinition;

public class StackHealthStateUpdateLock extends AbstractLockDefinition {

    public StackHealthStateUpdateLock(Environment stack) {
        super("STACK." + stack.getId() + "HEALTHSTATE.UPDATE");
    }

}
