package io.cattle.platform.process.common.lock;

import io.cattle.platform.lock.definition.AbstractBlockingLockDefintion;

public class AccountLinksUpdateLock extends AbstractBlockingLockDefintion {

    public AccountLinksUpdateLock(long accountId) {
        super("ACCOUNT.LINKS.UPDATE." + accountId);
    }

}