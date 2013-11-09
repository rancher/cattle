package io.github.ibuildthecloud.dstack.lock.definition;

// Quite purposefully not public, create your own lock
class DefaultLockDefinition extends AbstractLockDefinition {
    public DefaultLockDefinition(String lockId) {
        super(lockId);
    }
}
