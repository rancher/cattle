package io.github.ibuildthecloud.dstack.lock.impl;

import io.github.ibuildthecloud.dstack.lock.definition.AbstractMultiLockDefinition;
import io.github.ibuildthecloud.dstack.lock.definition.LockDefinition;

public class TestMultiLockDefinition extends AbstractMultiLockDefinition {

    public TestMultiLockDefinition(LockDefinition... lockDefinitions) {
        super(lockDefinitions);
    }

    public TestMultiLockDefinition(String... ids) {
        super(ids);
    }

}
