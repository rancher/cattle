package io.github.ibuildthecloud.dstack.lock.provider.impl;

import io.github.ibuildthecloud.dstack.lock.Lock;
import io.github.ibuildthecloud.dstack.lock.definition.BlockingLockDefinition;
import io.github.ibuildthecloud.dstack.lock.definition.LockDefinition;
import io.github.ibuildthecloud.dstack.lock.exception.FailedToAcquireLockException;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StandardLock implements Lock {

    private static final Logger log = LoggerFactory.getLogger(StandardLock.class);

    LockDefinition lockDefinition;
    java.util.concurrent.locks.Lock lock;
    long timeout;
    long referenceCount = 0;

    public StandardLock(LockDefinition lockDefinition, java.util.concurrent.locks.Lock lock) {
        super();
        this.lockDefinition = lockDefinition;
        this.lock = lock;
        if ( lockDefinition instanceof BlockingLockDefinition ) {
            this.timeout = ((BlockingLockDefinition)lockDefinition).getWait();
        }
    }


    @Override
    public boolean tryLock() {
        return lock.tryLock();
    }

    @Override
    public void lock() throws FailedToAcquireLockException {
        try {
            if ( ! lock.tryLock(timeout, TimeUnit.MILLISECONDS) )
                throw new FailedToAcquireLockException(lockDefinition);
        } catch (InterruptedException e) {
            log.error("Failed to lock [{}], interrupted", lockDefinition, e);
            throw new FailedToAcquireLockException(lockDefinition);
        }
    }

    @Override
    public void unlock() {
        try {
            lock.unlock();
        } catch ( Throwable t ) {
            log.trace("Failed to unlock [{}], may not own lock", lockDefinition, t);
        }
    }

    @Override
    public LockDefinition getLockDefinition() {
        return lockDefinition;
    }

    public long getReference() {
        return referenceCount;
    }

    public long incrementReference() {
        return ++referenceCount;
    }

    public long decrementReference() {
        return --referenceCount;
    }
}
