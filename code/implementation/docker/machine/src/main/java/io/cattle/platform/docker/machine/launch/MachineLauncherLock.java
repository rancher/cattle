package io.cattle.platform.docker.machine.launch;

import io.cattle.platform.lock.definition.AbstractLockDefinition;
import io.cattle.platform.lock.definition.LockDefinition;

public class MachineLauncherLock extends AbstractLockDefinition implements LockDefinition {

    public MachineLauncherLock() {
        super("MACHINE.LAUNCH");
    }

}
