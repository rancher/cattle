package io.cattle.platform.lock.definition;

// Quite purposefully not public, create your own lock
class DefaultBlockingLockDefinition extends AbstractLockDefinition implements BlockingLockDefinition {
    long wait;

    public DefaultBlockingLockDefinition(String lockId, long wait) {
        super(lockId);
        this.wait = wait;
    }

    @Override
    public long getWait() {
        return wait;
    }
}
