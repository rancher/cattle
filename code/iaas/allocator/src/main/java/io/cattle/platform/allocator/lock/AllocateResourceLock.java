package io.cattle.platform.allocator.lock;

import io.cattle.platform.allocator.service.AllocationRequest;
import io.cattle.platform.lock.definition.AbstractLockDefinition;

public class AllocateResourceLock extends AbstractLockDefinition {

    public AllocateResourceLock(AllocationRequest.Type type, Long resourceId) {
        super("ALLOCATE.RESOURCE." + type + "." + resourceId);
    }

    public AllocateResourceLock(AllocationRequest request) {
        this(request.getType(), request.getResourceId());
    }

}
