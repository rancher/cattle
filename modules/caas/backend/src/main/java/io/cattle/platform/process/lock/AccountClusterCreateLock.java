package io.cattle.platform.process.lock;

import io.cattle.platform.lock.definition.AbstractBlockingLockDefintion;

public class AccountClusterCreateLock extends AbstractBlockingLockDefintion {

    public AccountClusterCreateLock(long accountId) {
        super("ACCOUNT.CLUSTER." + accountId);
    }

}
