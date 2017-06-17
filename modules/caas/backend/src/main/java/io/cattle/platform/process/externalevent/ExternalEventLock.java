package io.cattle.platform.process.externalevent;

import io.cattle.platform.lock.definition.AbstractBlockingLockDefintion;

public class ExternalEventLock extends AbstractBlockingLockDefintion {

    public ExternalEventLock(String type, long accountId, String externalId) {
        super("EXTERNALEVENT." + type + "." + accountId + "." + externalId.hashCode());
    }

}
