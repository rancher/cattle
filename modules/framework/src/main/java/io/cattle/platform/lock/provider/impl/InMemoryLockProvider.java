package io.cattle.platform.lock.provider.impl;

import io.cattle.platform.lock.definition.LockDefinition;

import java.util.concurrent.locks.ReentrantLock;

public class InMemoryLockProvider extends AbstractStandardLockProvider {

    @Override
    protected StandardLock createLock(LockDefinition lockDefinition) {
        return new StandardLock(lockDefinition, new ReentrantLock());
    }

}
