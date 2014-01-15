package io.github.ibuildthecloud.dstack.allocator.lock;

import io.github.ibuildthecloud.dstack.allocator.service.AllocationRequest;
import io.github.ibuildthecloud.dstack.lock.definition.AbstractLockDefinition;

public class AllocateResourceLock extends AbstractLockDefinition {

    public AllocateResourceLock(AllocationRequest.Type type, Long resourceId) {
        super("ALLOCATE.RESOURCE." + type + "." + resourceId);
    }

    public AllocateResourceLock(AllocationRequest request) {
        this(request.getType(), request.getResourceId());
    }

}
