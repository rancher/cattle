package io.cattle.platform.lock.provider.impl;

import io.cattle.platform.lock.Lock;
import io.cattle.platform.lock.definition.BlockingLockDefinition;
import io.cattle.platform.lock.definition.LockDefinition;
import io.cattle.platform.lock.exception.FailedToAcquireLockException;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StandardLock implements Lock {

    private static final Logger log = LoggerFactory.getLogger(StandardLock.class);

    LockDefinition lockDefinition;
    java.util.concurrent.locks.Lock lock;
    long timeout;
    volatile long referenceCount = 0;
    volatile Thread owner = null;
    long ownerCount = 0;
    boolean acquired = false;

    public StandardLock(LockDefinition lockDefinition, java.util.concurrent.locks.Lock lock) {
        this.lockDefinition = lockDefinition;
        this.lock = lock;
        if (lockDefinition instanceof BlockingLockDefinition) {
            this.timeout = ((BlockingLockDefinition) lockDefinition).getWait();
        }
    }

    @Override
    public boolean tryLock() {
        log.trace("Try Lock Attempt [{}]", timeout);
        boolean result = lock.tryLock();
        log.trace("Try Lock [{}] result [{}]", lockDefinition, result);

        if (result) {
            incrementOwner();
        }

        return result;
    }

    public boolean wasAcquired() {
        return acquired;
    }

    @Override
    public void lock() throws FailedToAcquireLockException {
        log.trace("Lock Attempt [{}], timeout [{}]", lockDefinition, timeout);
        try {
            if (!doLock()) {
                log.trace("Lock [{}], timeout [{}] failed", lockDefinition, timeout);
                throw new FailedToAcquireLockException(lockDefinition);
            }
        } catch (InterruptedException e) {
            log.error("Failed to lock [{}], interrupted", lockDefinition, e);
            throw new FailedToAcquireLockException(lockDefinition);
        }

        incrementOwner();
        log.trace("Lock [{}] owner", lockDefinition);
    }

    protected void incrementOwner() {
        acquired = true;
        ownerCount++;
        if (owner == null) {
            log.trace("Lock [{}] acquiring ownernship count [{}]", lockDefinition, ownerCount);
            owner = Thread.currentThread();
        } else {
            log.trace("Lock [{}] ownernship count [{}]", lockDefinition, ownerCount);
        }
    }

    protected void decrementOwner() {
        ownerCount--;
        if (ownerCount <= 0) {
            log.trace("Lock [{}] releasing ownernship count [{}]", lockDefinition, ownerCount);
            ownerCount = 0;
            owner = null;
        } else {
            log.trace("Lock [{}] ownernship count [{}]", lockDefinition, ownerCount);
        }
    }

    protected boolean doLock() throws InterruptedException {
        if (timeout <= 0) {
            return lock.tryLock();
        } else {
            return lock.tryLock(timeout, TimeUnit.MILLISECONDS);
        }
    }

    @Override
    public void unlock() {
        try {
            if (Thread.currentThread() == owner) {
                log.trace("Unlock [{}] owner", lockDefinition);
                decrementOwner();
                lock.unlock();
            } else {
                log.trace("Unlock [{}] not owner", lockDefinition);
            }
        } catch (Throwable t) {
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
        log.trace("Lock [{}] reference count increment [{}]", lockDefinition, referenceCount);
        return ++referenceCount;
    }

    public long decrementReference() {
        log.trace("Lock [{}] reference count decrement [{}]", lockDefinition, referenceCount);
        return --referenceCount;
    }

    public java.util.concurrent.locks.Lock getLock() {
        return lock;
    }
}
