package io.github.ibuildthecloud.dstack.lock.exception;

import io.github.ibuildthecloud.dstack.lock.definition.LockDefinition;

public class FailedToAcquireLockException extends RuntimeException {

    private static final long serialVersionUID = -2945365126537822189L;

    LockDefinition lockDefition;

    public FailedToAcquireLockException(LockDefinition lockDefinition) {
        super("Failed to acquire lock [" + (lockDefinition == null ? null : lockDefinition.getLockId()) + "]");
        this.lockDefition = lockDefinition;
    }

    public LockDefinition getLockDefition() {
        return lockDefition;
    }

}
