package io.cattle.platform.docker.process.containerevent;

import io.cattle.platform.lock.definition.AbstractBlockingLockDefintion;

public class ContainerEventInstanceLock extends AbstractBlockingLockDefintion {

    public ContainerEventInstanceLock(long accountId, String externalId) {
        super("CONTAINEREVENT.INSTANCE.CREATE." + accountId + "." + externalId);
    }

}
