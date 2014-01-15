package io.github.ibuildthecloud.dstack.lock.definition;


public class AbstractLockDefinition implements LockDefinition {

    String lockId;

    public AbstractLockDefinition(String lockId) {
        super();
        this.lockId = lockId;
    }

    @Override
    public String getLockId() {
        return lockId;
    }

    protected static class DefaultLockDefinition extends AbstractLockDefinition {
        public DefaultLockDefinition(String lockId) {
            super(lockId);
        }
    }
}
