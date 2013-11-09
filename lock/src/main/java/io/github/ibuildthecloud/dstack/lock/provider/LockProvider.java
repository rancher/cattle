package io.github.ibuildthecloud.dstack.lock.provider;


import io.github.ibuildthecloud.dstack.lock.Lock;
import io.github.ibuildthecloud.dstack.lock.definition.LockDefinition;
import io.github.ibuildthecloud.dstack.util.type.Named;

public interface LockProvider extends Named {

    Lock getLock(LockDefinition lockDefinition);

    void activate();

    void releaseLock(Lock lock);
}
