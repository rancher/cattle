package io.github.ibuildthecloud.dstack.lock.provider.impl;

import io.github.ibuildthecloud.dstack.lock.definition.LockDefinition;

import java.util.concurrent.locks.ReentrantLock;

public class InMemoryLockProvider extends AbstractStandardLockProvider {

    @Override
    protected StandardLock createLock(LockDefinition lockDefinition) {
        return new StandardLock(lockDefinition, new ReentrantLock());
    }

}
