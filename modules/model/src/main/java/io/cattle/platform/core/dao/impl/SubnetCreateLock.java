package io.cattle.platform.core.dao.impl;

import io.cattle.platform.lock.definition.AbstractBlockingLockDefintion;

public class SubnetCreateLock extends AbstractBlockingLockDefintion {

    public SubnetCreateLock(long accountId) {
        super("SUBNET.CREATE." + accountId);
    }
}
