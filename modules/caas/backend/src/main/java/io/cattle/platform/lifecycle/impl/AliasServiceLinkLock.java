package io.cattle.platform.lifecycle.impl;

import io.cattle.platform.lock.definition.AbstractBlockingLockDefintion;

public class AliasServiceLinkLock extends AbstractBlockingLockDefintion {

    public AliasServiceLinkLock (long serviceId) {
        super("ALIAS.SERVICE.LINK.UPDATE." + serviceId);
    }
}
