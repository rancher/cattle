package io.cattle.platform.servicediscovery.deployment.impl.lock;

import io.cattle.platform.core.model.Stack;
import io.cattle.platform.lock.definition.AbstractBlockingLockDefintion;

public class StackVolumeLock extends AbstractBlockingLockDefintion {

    public StackVolumeLock(Stack stack, String volumeName) {
        super("STACK.VOLUME." + stack.getId() + "." + volumeName);
    }

}
