package io.cattle.platform.allocator.lock;

import io.cattle.platform.lock.definition.AbstractLockDefinition;

public class AllocateConstraintLock extends AbstractLockDefinition {

    public AllocateConstraintLock(String constraintId) {
        super("ALLOCATE.CONSTRAINT." + constraintId);
    }
}
