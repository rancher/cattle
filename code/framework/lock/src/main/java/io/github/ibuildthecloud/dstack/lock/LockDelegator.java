package io.github.ibuildthecloud.dstack.lock;

import io.github.ibuildthecloud.dstack.lock.definition.LockDefinition;

public interface LockDelegator {

    boolean tryLock(LockDefinition lockDef);

    boolean unlock(LockDefinition lockDef);

    boolean isLocked(LockDefinition lockDef);

}
