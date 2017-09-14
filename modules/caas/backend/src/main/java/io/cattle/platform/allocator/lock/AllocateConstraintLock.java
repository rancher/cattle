package io.cattle.platform.allocator.lock;

import io.cattle.platform.lock.definition.AbstractBlockingLockDefintion;


public class AllocateConstraintLock extends AbstractBlockingLockDefintion {
    public enum Type {
        DEPLOYMENT_UNIT,
        AFFINITY,
        PORT,
        VOLUME
    }
    public AllocateConstraintLock(Type type, Object constraintId) {
        super("ALLOCATE.CONSTRAINT." + type + "." + constraintId);
    }
}
