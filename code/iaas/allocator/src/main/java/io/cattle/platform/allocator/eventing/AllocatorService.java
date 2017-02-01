package io.cattle.platform.allocator.eventing;

import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Volume;


public interface AllocatorService {

    void instanceAllocate(Instance instance);

    void instanceDeallocate(Instance instance);

    void volumeDeallocate(Volume volume);

    void ensureResourcesReleasedForStop(Instance instance);

    void ensureResourcesReservedForStart(Instance instance);
}
