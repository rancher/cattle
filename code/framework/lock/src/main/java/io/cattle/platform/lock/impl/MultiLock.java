package io.cattle.platform.lock.impl;

import io.cattle.platform.lock.Lock;
import io.cattle.platform.lock.definition.LockDefinition;
import io.cattle.platform.lock.definition.MultiLockDefinition;
import io.cattle.platform.lock.exception.FailedToAcquireLockException;
import io.cattle.platform.lock.provider.LockProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MultiLock implements Lock {

    private static final Logger log = LoggerFactory.getLogger(MultiLock.class);

    Lock[] locks;
    MultiLockDefinition definition;

    public MultiLock(MultiLockDefinition def, Lock... locks) {
        this.locks = locks;
        this.definition = def;
    }

    public MultiLock(MultiLockDefinition def, LockProvider provider) {
        this.definition = def;

        LockDefinition[] defs = def.getLockDefinitions();
        locks = new Lock[defs.length];

        for (int i = 0; i < defs.length; i++) {
            locks[i] = provider.getLock(defs[i]);
        }
    }

    public Lock[] getLocks() {
        return locks;
    }

    @Override
    public boolean tryLock() {
        for (Lock lock : locks) {
            if (!lock.tryLock())
                return false;
        }
        return true;
    }

    @Override
    public void lock() throws FailedToAcquireLockException {
        for (Lock lock : locks) {
            lock.lock();
        }
    }

    @Override
    public void unlock() {
        for (Lock lock : locks) {
            try {
                lock.unlock();
            } catch (Throwable t) {
                /*
                 * This is never supposed to happen, but hey sometime people
                 * don't program the right thing
                 */
                log.error("Failed to unlock [{}], unlock() should never throw an exception", lock.getLockDefinition().getLockId(), t);
            }
        }
    }

    @Override
    public LockDefinition getLockDefinition() {
        return definition;
    }

}
