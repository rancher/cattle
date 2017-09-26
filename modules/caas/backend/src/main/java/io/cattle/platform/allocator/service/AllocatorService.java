package io.cattle.platform.allocator.service;

import io.cattle.platform.core.model.Instance;


public interface AllocatorService {

    void instanceAllocate(Instance instance);

    void instanceDeallocate(Instance instance);

    void ensureResourcesReleasedForStop(Instance instance);

    void ensureResourcesReservedForStart(Instance instance);

}
