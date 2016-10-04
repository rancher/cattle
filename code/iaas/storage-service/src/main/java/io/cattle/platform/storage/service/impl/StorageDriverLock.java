package io.cattle.platform.storage.service.impl;

import io.cattle.platform.core.model.StorageDriver;
import io.cattle.platform.lock.definition.AbstractBlockingLockDefintion;

public class StorageDriverLock extends AbstractBlockingLockDefintion {

    public StorageDriverLock(StorageDriver driver) {
        super("STORAGE.DRIVER." + driver.getId());
    }

}
