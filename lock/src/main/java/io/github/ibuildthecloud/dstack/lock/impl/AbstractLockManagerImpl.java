package io.github.ibuildthecloud.dstack.lock.impl;

import io.github.ibuildthecloud.dstack.lock.Lock;
import io.github.ibuildthecloud.dstack.lock.LockCallback;
import io.github.ibuildthecloud.dstack.lock.LockCallbackWithException;
import io.github.ibuildthecloud.dstack.lock.LockManager;
import io.github.ibuildthecloud.dstack.lock.definition.LockDefinition;

public abstract class AbstractLockManagerImpl implements LockManager {

    @Override
    public <T,E extends Throwable> T lock(LockDefinition lockDef, LockCallbackWithException<T,E> callback, Class<E> clz) throws E {
        return doLock(lockDef, callback, new WithLock() {
            @Override
            public boolean withLock(Lock lock) {
                lock.lock();
                return true;
            }
        });
    }

    @Override
    public <T,E extends Throwable> T tryLock(LockDefinition lockDef, LockCallbackWithException<T,E> callback, Class<E> clz) throws E {
        return doLock(lockDef, callback, new WithLock() {
            @Override
            public boolean withLock(Lock lock) {
                return lock.tryLock();
            }
        });
    }

    @Override
    public <T> T lock(LockDefinition lockDef, final LockCallback<T> callback) {
        return lock(lockDef, new LockCallbackWithException<T, RuntimeException>() {
            @Override
            public T doWithLock() throws RuntimeException {
                return callback.doWithLock();
            }
        }, RuntimeException.class);
    }

    @Override
    public <T> T tryLock(LockDefinition lockDef, final LockCallback<T> callback) {
        return tryLock(lockDef, new LockCallbackWithException<T, RuntimeException>() {
            @Override
            public T doWithLock() throws RuntimeException {
                return callback.doWithLock();
            }
        }, RuntimeException.class);
    }

    protected abstract <T,E extends Throwable> T doLock(LockDefinition lockDef, 
            LockCallbackWithException<T,E> callback, WithLock with) throws E;

    protected static interface WithLock {
        boolean withLock(Lock lock);
    }

}
