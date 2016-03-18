package io.cattle.platform.docker.process.lock;

import io.cattle.platform.lock.definition.AbstractBlockingLockDefintion;

public class ComposeProjectLock extends AbstractBlockingLockDefintion {

    public ComposeProjectLock(long accountId, String name) {
        super(String.format("COMPOSE.PROJECT.%d.%s", accountId, name));
    }

}