package io.cattle.platform.systemstack.lock;

import io.cattle.platform.lock.definition.AbstractBlockingLockDefintion;

public class ScheduledUpgradeLock extends AbstractBlockingLockDefintion {

    public ScheduledUpgradeLock() {
        super("SCHEDULED.UPGRADE");
    }

}
