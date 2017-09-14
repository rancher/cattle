package io.cattle.platform.lock.provider.impl;

import io.cattle.platform.lock.Lock;
import io.cattle.platform.lock.definition.LockDefinition;
import io.cattle.platform.lock.provider.LockProvider;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractStandardLockProvider implements LockProvider {

    private static final Logger log = LoggerFactory.getLogger(AbstractStandardLockProvider.class);

    Map<String, StandardLock> locks = new HashMap<String, StandardLock>();
    boolean referenceCountLocks = true;

    @Override
    public String getName() {
        return getClass().getSimpleName();
    }

    @Override
    public synchronized Lock getLock(LockDefinition lockDefinition) {
        if (lockDefinition == null || lockDefinition.getLockId() == null)
            return null;

        if (!referenceCountLocks) {
            return createLock(lockDefinition);
        }

        StandardLock lock = locks.get(lockDefinition.getLockId());

        if (lock == null) {
            lock = createLock(lockDefinition);
            locks.put(lockDefinition.getLockId(), lock);
        }

        lock.incrementReference();
        return lock;
    }

    protected abstract StandardLock createLock(LockDefinition lockDefinition);

    @Override
    public synchronized void releaseLock(Lock lock) {
        if (lock instanceof StandardLock) {
            StandardLock sLock = (StandardLock) lock;

            if (!referenceCountLocks) {
                destroyLock(sLock);
                return;
            }

            long count = sLock.decrementReference();
            if (count <= 0) {
                destroyLock(sLock);
                locks.remove(lock.getLockDefinition().getLockId());
                if (count < 0) {
                    log.error("Reference count is not zero this should not happened and it is a bug, count [{}]", count);
                }
            }
        } else {
            throw new IllegalStateException("Lock [" + lock + "] not created by this provider");
        }
    }

    protected void destroyLock(StandardLock lock) {
    }

    public boolean isReferenceCountLocks() {
        return referenceCountLocks;
    }

    public void setReferenceCountLocks(boolean referenceCountLocks) {
        this.referenceCountLocks = referenceCountLocks;
    }

}
