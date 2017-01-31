package io.cattle.platform.allocator.service;

import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Volume;

public interface Allocator {

    public void allocate(Instance instance);

    public void deallocate(Instance instance);

    public void deallocate(Volume volume);

    /**
     * Ensure allocated resources are released without doing a full
     * deallocate (i.e. release ports).
     * @param instance
     * @param process The process for which this is being requested (i.e. instance.stop)
     */
    void ensureResourcesReleased(Instance instance);

    /**
     * Ensure allocated resources are reserved after having being previously allocated
     * (i.e. during an instance.start after an instance.stop)
     * @param instance
     * @param process The process for which this is being requested (i.e. instance.stop)
     */
    void ensureResourcesReserved(Instance instance);

}