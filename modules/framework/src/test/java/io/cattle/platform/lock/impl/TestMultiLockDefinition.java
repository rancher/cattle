package io.cattle.platform.lock.impl;

import io.cattle.platform.lock.definition.AbstractMultiLockDefinition;
import io.cattle.platform.lock.definition.LockDefinition;

public class TestMultiLockDefinition extends AbstractMultiLockDefinition {

    public TestMultiLockDefinition(LockDefinition... lockDefinitions) {
        super(lockDefinitions);
    }

    public TestMultiLockDefinition(String... ids) {
        super(ids);
    }

}
