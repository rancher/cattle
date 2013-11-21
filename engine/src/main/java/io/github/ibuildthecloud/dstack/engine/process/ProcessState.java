package io.github.ibuildthecloud.dstack.engine.process;

import io.github.ibuildthecloud.dstack.lock.definition.LockDefinition;

public interface ProcessState extends ProcessStateOperations {

    Object getResource();

    LockDefinition getProcessLock();

//    LockDefinition getStateChangeLock();

    String getState();

    boolean shouldCancel();

    boolean isDone();

    boolean isStart();

    boolean isTransitioning();

    void reload();

}
