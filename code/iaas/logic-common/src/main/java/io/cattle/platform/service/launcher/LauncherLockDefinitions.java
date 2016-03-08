package io.cattle.platform.service.launcher;

import io.cattle.platform.lock.definition.AbstractLockDefinition;
import io.cattle.platform.lock.definition.LockDefinition;

public class LauncherLockDefinitions extends AbstractLockDefinition implements LockDefinition {

    private LauncherLockDefinitions(String lockId) {
        super(lockId);
    }

    public static LockDefinition MachineLauncherLock() {
        return new LauncherLockDefinitions("MACHINE.LAUNCH");
    }
    
    public static LockDefinition ComposeExecutorLauncherLock() {
        return new LauncherLockDefinitions("COMPOSE.EXECUTOR.LAUNCH");
    }

}
