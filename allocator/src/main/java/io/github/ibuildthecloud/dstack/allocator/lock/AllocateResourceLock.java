package io.github.ibuildthecloud.dstack.allocator.lock;

import io.github.ibuildthecloud.dstack.allocator.service.AllocationRequest;
import io.github.ibuildthecloud.dstack.lock.definition.AbstractLockDefinition;

public class AllocateResourceLock extends AbstractLockDefinition {

    public AllocateResourceLock(AllocationRequest request) {
        super("ALLOCATE.RESOURCE." + request.getType() + "." + request.getResourceId());
    }

}
