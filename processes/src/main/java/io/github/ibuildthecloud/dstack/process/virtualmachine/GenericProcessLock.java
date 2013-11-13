package io.github.ibuildthecloud.dstack.process.virtualmachine;

import io.github.ibuildthecloud.dstack.lock.definition.AbstractLockDefinition;

public class GenericProcessLock extends AbstractLockDefinition {

    public GenericProcessLock(String resourceType, String resourceId) {
        super(resourceType + "." + resourceId + ".PROCESS");
    }

}
