package io.github.ibuildthecloud.dstack.process.lock;

import java.util.UUID;

import io.github.ibuildthecloud.dstack.lock.definition.AbstractLockDefinition;

public class UniqueStateChangeLock extends AbstractLockDefinition {

    public UniqueStateChangeLock() {
        super(UUID.randomUUID().toString());
    }

}
