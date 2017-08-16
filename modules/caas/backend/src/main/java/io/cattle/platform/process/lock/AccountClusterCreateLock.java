package io.cattle.platform.process.lock;

import io.cattle.platform.core.model.Account;
import io.cattle.platform.lock.definition.AbstractBlockingLockDefintion;

public class AccountClusterCreateLock extends AbstractBlockingLockDefintion {

    public AccountClusterCreateLock(Account account) {
        super("ACCOUNT.CLUSTER." + account.getId());
    }

}
