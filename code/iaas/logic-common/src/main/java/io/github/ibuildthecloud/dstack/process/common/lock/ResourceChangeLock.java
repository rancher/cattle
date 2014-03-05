package io.github.ibuildthecloud.dstack.process.common.lock;

import io.github.ibuildthecloud.dstack.lock.definition.AbstractLockDefinition;
import io.github.ibuildthecloud.dstack.lock.definition.BlockingLockDefinition;

public class ResourceChangeLock extends AbstractLockDefinition implements BlockingLockDefinition {

    public ResourceChangeLock(String type, Object id) {
        super(type + "." + id.toString() + ".CHANGE");
    }

    @Override
    public long getWait() {
        return 1000;
    }

}
