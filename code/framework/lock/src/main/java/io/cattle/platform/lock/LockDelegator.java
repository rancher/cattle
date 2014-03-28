package io.cattle.platform.lock;

import io.cattle.platform.lock.definition.LockDefinition;

public interface LockDelegator {

    boolean tryLock(LockDefinition lockDef);

    boolean unlock(LockDefinition lockDef);

    boolean isLocked(LockDefinition lockDef);

}
