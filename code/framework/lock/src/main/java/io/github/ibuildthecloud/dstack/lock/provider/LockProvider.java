package io.github.ibuildthecloud.dstack.lock.provider;


import io.github.ibuildthecloud.dstack.lock.Lock;
import io.github.ibuildthecloud.dstack.lock.definition.LockDefinition;
import io.github.ibuildthecloud.dstack.util.type.Named;

public interface LockProvider extends Named {

    /**
     * Provides the lock for the {@link LockDefinition}.
     *
     * @param lockDefinition
     * @return A {@link Lock} or null if lockDefinition == null or lockDefinition.getLockId() == null
     */
    Lock getLock(LockDefinition lockDefinition);

    void releaseLock(Lock lock);
}
