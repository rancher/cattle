package io.cattle.platform.allocator.service;

import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Volume;

public interface Allocator {

    public void allocate(Instance instance);

    public void deallocate(Instance instance);

    public void deallocate(Volume volume);

}