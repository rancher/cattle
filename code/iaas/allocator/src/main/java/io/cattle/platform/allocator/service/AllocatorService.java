package io.cattle.platform.allocator.service;

import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Port;
import io.cattle.platform.core.model.Volume;

import java.util.List;
import java.util.Map;


public interface AllocatorService {

    void instanceAllocate(Instance instance);

    void instanceDeallocate(Instance instance);

    void volumeDeallocate(Volume volume);

    void ensureResourcesReleasedForStop(Instance instance);

    void ensureResourcesReservedForStart(Instance instance);

    void allocatePortsForInstanceUpdate(Instance instance, List<Port> newPorts);

    void releasePortsForInstanceUpdate(Instance instance, List<Port> removedPorts);

    List<String> callExternalSchedulerForHostsSatisfyingLabels(Long accountId, Map<String, String> labels);
}
