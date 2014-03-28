package io.cattle.platform.storage.simulator.lock;

import io.cattle.platform.lock.definition.AbstractLockDefinition;
import io.cattle.platform.lock.definition.BlockingLockDefinition;

public class SimExtPoolCreateLock extends AbstractLockDefinition implements BlockingLockDefinition {

    public SimExtPoolCreateLock() {
        super("SIM.EXT.POOL.CREATE");
    }

    @Override
    public long getWait() {
        return 1000;
    }

}
