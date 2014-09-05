package io.cattle.platform.sample.lock;

import io.cattle.platform.lock.definition.AbstractBlockingLockDefintion;

public class SampleDataLock extends AbstractBlockingLockDefintion {

    public SampleDataLock() {
        super("SAMPLE.DATA.LOCK");
    }

}
