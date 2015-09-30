package io.cattle.platform.process.common.lock;

import io.cattle.platform.lock.definition.AbstractLockDefinition;
import io.cattle.platform.lock.definition.BlockingLockDefinition;

public class ResourceChangeLock extends AbstractLockDefinition implements BlockingLockDefinition {

    public ResourceChangeLock(String type, Object id) {
        super(type + "." + id.toString() + ".CHANGE");
    }

    @Override
    public long getWait() {
        return 3000;
    }

}
