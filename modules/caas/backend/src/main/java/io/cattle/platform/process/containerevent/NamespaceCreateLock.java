package io.cattle.platform.process.containerevent;

import io.cattle.platform.lock.definition.AbstractBlockingLockDefintion;

public class NamespaceCreateLock extends AbstractBlockingLockDefintion {

    public NamespaceCreateLock(long clusterId, String namespace) {
        super("NAMESPACE.CREATE." + clusterId + "." + namespace.hashCode());
    }

}
