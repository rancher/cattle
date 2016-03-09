package io.cattle.platform.service.launcher;

import io.cattle.platform.lock.definition.AbstractBlockingLockDefintion;

public class ServiceCredLock extends AbstractBlockingLockDefintion {

    public ServiceCredLock(String uuid) {
        super("SERVICE.ACCOUNT.CREATE." + uuid);
    }

}
