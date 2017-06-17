package io.cattle.platform.data.lock;

import io.cattle.platform.lock.definition.AbstractBlockingLockDefintion;

public class InitialDataLock extends AbstractBlockingLockDefintion {

    public InitialDataLock() {
        super("INITIAL.DATA.LOCK");
    }

}
