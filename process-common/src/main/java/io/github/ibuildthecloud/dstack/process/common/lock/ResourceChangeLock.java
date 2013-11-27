package io.github.ibuildthecloud.dstack.process.common.lock;

import io.github.ibuildthecloud.dstack.lock.definition.AbstractLockDefinition;

public class ResourceChangeLock extends AbstractLockDefinition {

    public ResourceChangeLock(String type, Object id) {
        super(type + "." + id.toString() + ".CHANGE");
    }

}
