package io.cattle.platform.lock.definition;

public class DefaultMultiLockDefinition extends AbstractMultiLockDefinition {

    public DefaultMultiLockDefinition(LockDefinition... lockDefinitions) {
        super(lockDefinitions);
    }

    public DefaultMultiLockDefinition(String... ids) {
        super(ids);
    }

}
