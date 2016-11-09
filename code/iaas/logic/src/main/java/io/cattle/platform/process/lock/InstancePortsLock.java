package io.cattle.platform.process.lock;

import io.cattle.platform.core.model.Instance;
import io.cattle.platform.lock.definition.AbstractLockDefinition;

public class InstancePortsLock extends AbstractLockDefinition {

    public InstancePortsLock(Instance instance) {
        super("INSTANCE.PORTS." + instance.getId());
    }

}
