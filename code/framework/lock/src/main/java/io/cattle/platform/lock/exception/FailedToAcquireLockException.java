package io.cattle.platform.lock.exception;

import io.cattle.platform.lock.definition.LockDefinition;

public class FailedToAcquireLockException extends RuntimeException {

    private static final long serialVersionUID = -2945365126537822189L;

    LockDefinition lockDefition;

    public FailedToAcquireLockException(LockDefinition lockDefinition) {
        super("Failed to acquire lock [" + (lockDefinition == null ? null : lockDefinition.getLockId()) + "]");
        this.lockDefition = lockDefinition;
    }

    public boolean isLock(LockDefinition lockDef) {
        if (lockDef == null) {
            return lockDefition == null;
        } else if (lockDefition == null) {
            return false;
        }
        return lockDef.getLockId().equals(lockDefition.getLockId());
    }

    public String getLockId() {
        if (lockDefition == null) {
            return null;
        }
        return lockDefition.getLockId();
    }

}
