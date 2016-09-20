package io.cattle.platform.servicediscovery.deployment.impl.lock;

import io.cattle.platform.core.model.Stack;
import io.cattle.platform.lock.definition.AbstractLockDefinition;

public class StackVolumeLock extends AbstractLockDefinition {

    public StackVolumeLock(Stack stack) {
        super("SERVICE.VOLUME." + stack.getId());
    }

}
