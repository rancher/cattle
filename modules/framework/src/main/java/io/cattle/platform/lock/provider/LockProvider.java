package io.cattle.platform.lock.provider;

import io.cattle.platform.lock.Lock;
import io.cattle.platform.lock.definition.LockDefinition;
import io.cattle.platform.util.type.Named;

public interface LockProvider extends Named {

    /**
     * Provides the lock for the {@link LockDefinition}.
     *
     * @param lockDefinition
     * @return A {@link Lock} or null if lockDefinition == null or
     *         lockDefinition.getLockId() == null
     */
    Lock getLock(LockDefinition lockDefinition);

    void releaseLock(Lock lock);
}
