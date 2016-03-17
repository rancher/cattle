package io.cattle.platform.docker.process.lock;

import io.cattle.platform.lock.definition.AbstractBlockingLockDefintion;

public class ComposeServiceLock extends AbstractBlockingLockDefintion {

    public ComposeServiceLock(long accountId, String name) {
        super(String.format("COMPOSE.SERVICE.%d.%s", accountId, name));
    }

}
