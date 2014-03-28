package io.cattle.platform.lock;

import io.cattle.platform.lock.definition.LockDefinition;
import io.cattle.platform.lock.exception.FailedToAcquireLockException;

public interface Lock {

    boolean tryLock();

    void lock() throws FailedToAcquireLockException;

    void unlock();

    LockDefinition getLockDefinition();

}
