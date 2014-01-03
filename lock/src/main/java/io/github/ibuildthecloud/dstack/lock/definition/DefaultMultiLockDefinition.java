package io.github.ibuildthecloud.dstack.lock.definition;

// Quite purposefully not public, create your own lock
class DefaultMultiLockDefinition extends AbstractMultiLockDefinition {

    public DefaultMultiLockDefinition(LockDefinition... lockDefinitions) {
        super(lockDefinitions);
    }

    public DefaultMultiLockDefinition(String... ids) {
        super(ids);
    }

}
