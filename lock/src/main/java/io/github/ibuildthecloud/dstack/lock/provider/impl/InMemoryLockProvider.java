package io.github.ibuildthecloud.dstack.lock.provider.impl;

import io.github.ibuildthecloud.dstack.lock.Lock;
import io.github.ibuildthecloud.dstack.lock.definition.LockDefinition;
import io.github.ibuildthecloud.dstack.lock.provider.LockProvider;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

public class InMemoryLockProvider implements LockProvider {

    Map<String, StandardLock> locks = new HashMap<String, StandardLock>();

    @Override
    public String getName() {
        return getClass().getSimpleName();
    }

    @Override
    public synchronized Lock getLock(LockDefinition lockDefinition) {
        StandardLock lock = locks.get(lockDefinition.getLockId());
        
        if ( lock == null ) {
            lock = new StandardLock(lockDefinition, new ReentrantLock());
        }
        
        lock.incrementReference();
        return lock;
    }

    @Override
    public void activate() {
    }

    @Override
    public synchronized void releaseLock(Lock lock) {
        if ( lock instanceof StandardLock ) {
            StandardLock sLock = (StandardLock)lock;
            if ( sLock.decrementReference() <= 0 ) {
                locks.remove(lock.getLockDefinition().getLockId());
            }
        } else {
            throw new IllegalStateException("Lock [" + lock + "] not created by this provider");
        }
    }

}
