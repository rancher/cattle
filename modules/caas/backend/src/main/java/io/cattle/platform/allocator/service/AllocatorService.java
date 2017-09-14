package io.cattle.platform.allocator.service;

import io.cattle.platform.core.model.Instance;

import java.util.List;
import java.util.Map;


public interface AllocatorService {

    void instanceAllocate(Instance instance);

    void instanceDeallocate(Instance instance);

    void ensureResourcesReleasedForStop(Instance instance);

    void ensureResourcesReservedForStart(Instance instance);

    List<String> callExternalSchedulerForHostsSatisfyingLabels(Long accountId, Map<String, String> labels);
}
