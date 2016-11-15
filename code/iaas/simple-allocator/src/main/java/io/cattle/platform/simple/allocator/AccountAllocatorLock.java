package io.cattle.platform.simple.allocator;

import io.cattle.platform.lock.definition.AbstractLockDefinition;

public class AccountAllocatorLock extends AbstractLockDefinition {

    public AccountAllocatorLock(long accountId) {
        super("ALLOCATOR.LOCK.ACCOUNT." + accountId);
    }

}
