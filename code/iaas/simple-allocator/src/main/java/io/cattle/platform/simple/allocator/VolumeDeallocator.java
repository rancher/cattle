package io.cattle.platform.simple.allocator;

import io.cattle.platform.core.model.Volume;

public interface VolumeDeallocator {
    void releaseAllocation(Volume volume);
}
