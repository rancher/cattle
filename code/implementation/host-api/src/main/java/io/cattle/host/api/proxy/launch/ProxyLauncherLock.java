package io.cattle.host.api.proxy.launch;

import io.cattle.platform.lock.definition.AbstractLockDefinition;
import io.cattle.platform.lock.definition.LockDefinition;

public class ProxyLauncherLock extends AbstractLockDefinition implements LockDefinition {

    public ProxyLauncherLock() {
        super("PROXY.LAUNCH");
    }
}
