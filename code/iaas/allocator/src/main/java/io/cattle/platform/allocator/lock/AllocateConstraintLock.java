package io.cattle.platform.allocator.lock;

import io.cattle.platform.lock.definition.AbstractLockDefinition;

public class AllocateConstraintLock extends AbstractLockDefinition {
    public enum Type {
        DEPLOYMENT_UNIT,
        AFFINITY,
        PORT
    }
    public AllocateConstraintLock(Type type, String constraintId) {
        super("ALLOCATE.CONSTRAINT." + type + "." + constraintId);
    }
}
