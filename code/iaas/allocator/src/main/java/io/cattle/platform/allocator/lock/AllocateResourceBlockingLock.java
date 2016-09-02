package io.cattle.platform.allocator.lock;

import io.cattle.platform.allocator.service.AllocationRequest;
import io.cattle.platform.lock.definition.AbstractBlockingLockDefintion;

public class AllocateResourceBlockingLock extends AbstractBlockingLockDefintion {

    public AllocateResourceBlockingLock(AllocationRequest.Type type, Long resourceId) {
        super("ALLOCATE.RESOURCE." + type + "." + resourceId);
    }

    public AllocateResourceBlockingLock(AllocationRequest request) {
        this(request.getType(), request.getResourceId());
    }

}
