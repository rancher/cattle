package io.cattle.platform.lock.definition;

// Quite purposefully not public, create your own lock
class DefaultLockDefinition extends AbstractLockDefinition {
    public DefaultLockDefinition(String lockId) {
        super(lockId);
    }
}
