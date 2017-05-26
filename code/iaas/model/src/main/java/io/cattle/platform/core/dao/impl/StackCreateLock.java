package io.cattle.platform.core.dao.impl;

import io.cattle.platform.lock.definition.AbstractBlockingLockDefintion;

public class StackCreateLock extends AbstractBlockingLockDefintion {

    public StackCreateLock(long accountId) {
        super("STACK.CREATE." + accountId);
    }
}
