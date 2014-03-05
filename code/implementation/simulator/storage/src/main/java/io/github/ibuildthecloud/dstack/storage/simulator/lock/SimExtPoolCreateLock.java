package io.github.ibuildthecloud.dstack.storage.simulator.lock;

import io.github.ibuildthecloud.dstack.lock.definition.AbstractLockDefinition;
import io.github.ibuildthecloud.dstack.lock.definition.BlockingLockDefinition;

public class SimExtPoolCreateLock extends AbstractLockDefinition implements BlockingLockDefinition {

    public SimExtPoolCreateLock() {
        super("SIM.EXT.POOL.CREATE");
    }

    @Override
    public long getWait() {
        return 1000;
    }

}
