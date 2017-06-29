package io.cattle.platform.systemstack.lock;

import io.cattle.platform.lock.definition.AbstractBlockingLockDefintion;

public class SystemStackLock extends AbstractBlockingLockDefintion {

    public SystemStackLock(Long account) {
        super("SYSTEM.STACKS." + account);
    }

}
