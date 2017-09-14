package io.cattle.platform.launcher;

import io.cattle.platform.lock.definition.AbstractLockDefinition;
import io.cattle.platform.lock.definition.LockDefinition;

public class MachineDriverLoaderLock extends AbstractLockDefinition implements LockDefinition {

    private static final String LOCK_NAME = "MACHINE.DRIVER.LOADER";

    public MachineDriverLoaderLock() {
        super(LOCK_NAME);
    }
}
