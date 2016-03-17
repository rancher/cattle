package io.cattle.platform.process.common.lock;

import io.cattle.platform.lock.definition.AbstractLockDefinition;

public class ResourceChangeLock extends AbstractLockDefinition {

    public ResourceChangeLock(String type, Object id) {
        super(type + "." + id.toString() + ".CHANGE");
    }

}
