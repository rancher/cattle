package io.github.ibuildthecloud.dstack.process.virtualmachine;

import java.util.UUID;

import io.github.ibuildthecloud.dstack.lock.definition.AbstractLockDefinition;

public class GenericStateChangeLock extends AbstractLockDefinition {

    public GenericStateChangeLock(String resourceType, String resourceId) {
        super("STATE.CHANGE." + resourceType + "." + resourceId + "." + UUID.randomUUID().toString().replace("-", ""));
    }

}
