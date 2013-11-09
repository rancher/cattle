package io.github.ibuildthecloud.dstack.engine.process;

import io.github.ibuildthecloud.dstack.lock.definition.LockDefinition;

public interface ProcessState extends ProcessStateOperations {

    LockDefinition getProcessLock();

    LockDefinition getStateChangeLock();

    boolean shouldCancel();

    boolean isActive();

    boolean isInactive();

    boolean isActivating();

    void reload();

}
