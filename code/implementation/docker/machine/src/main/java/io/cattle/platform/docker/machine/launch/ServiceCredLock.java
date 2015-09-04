package io.cattle.platform.docker.machine.launch;

import io.cattle.platform.lock.definition.AbstractBlockingLockDefintion;

public class ServiceCredLock extends AbstractBlockingLockDefintion {

    public ServiceCredLock(String uuid) {
        super("SERVICE.ACCOUNT.CREATE." + uuid);
    }

}
