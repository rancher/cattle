package io.github.ibuildthecloud.dstack.process.lock;

import io.github.ibuildthecloud.dstack.lock.definition.AbstractLockDefinition;

public class ResourceLock extends AbstractLockDefinition {

    public ResourceLock(String type, Object id) {
        super(type + "." + id.toString());
    }

}
