package io.cattle.platform.simple.allocator;

import io.cattle.platform.lock.definition.AbstractBlockingLockDefintion;

public class AccountAllocatorLock extends AbstractBlockingLockDefintion {

    public AccountAllocatorLock(long accountId) {
        super("ALLOCATOR.LOCK.ACCOUNT." + accountId);
    }

}
