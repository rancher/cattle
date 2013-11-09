package io.github.ibuildthecloud.dstack.lock;

import io.github.ibuildthecloud.dstack.lock.definition.LockDefinition;
import io.github.ibuildthecloud.dstack.lock.exception.FailedToAcquireLockException;

public interface Lock {

    boolean tryLock();

    void lock() throws FailedToAcquireLockException;

    void unlock();

    LockDefinition getLockDefinition();

}
