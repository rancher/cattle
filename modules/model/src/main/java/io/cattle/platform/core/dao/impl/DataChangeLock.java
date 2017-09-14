package io.cattle.platform.core.dao.impl;

import io.cattle.platform.lock.definition.AbstractBlockingLockDefintion;

public class DataChangeLock extends AbstractBlockingLockDefintion {

    public DataChangeLock(String key) {
        super("DATA.CHANGE." + key);
    }

}
