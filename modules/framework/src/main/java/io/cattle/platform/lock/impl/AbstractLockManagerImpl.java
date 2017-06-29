package io.cattle.platform.lock.impl;

import io.cattle.platform.lock.Lock;
import io.cattle.platform.lock.LockCallback;
import io.cattle.platform.lock.LockCallbackWithException;
import io.cattle.platform.lock.LockManager;
import io.cattle.platform.lock.definition.LockDefinition;

public abstract class AbstractLockManagerImpl implements LockManager {

    @Override
    public <T, E extends Throwable> T lock(LockDefinition lockDef, LockCallbackWithException<T, E> callback, Class<E> clz) throws E {
        return doLock(lockDef, callback, new WithLock() {
            @Override
            public boolean withLock(Lock lock) {
                if (lock != null)
                    lock.lock();
                return true;
            }
        });
    }

    @Override
    public <T, E extends Throwable> T tryLock(LockDefinition lockDef, LockCallbackWithException<T, E> callback, Class<E> clz) throws E {
        return doLock(lockDef, callback, new WithLock() {
            @Override
            public boolean withLock(Lock lock) {
                if (lock != null)
                    return lock.tryLock();
                return true;
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

    protected abstract <T, E extends Throwable> T doLock(LockDefinition lockDef, LockCallbackWithException<T, E> callback, WithLock with) throws E;

    protected static interface WithLock {
        boolean withLock(Lock lock);
    }

}
