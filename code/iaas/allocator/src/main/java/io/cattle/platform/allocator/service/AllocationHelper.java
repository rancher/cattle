package io.cattle.platform.allocator.service;

import io.cattle.platform.allocator.constraint.Constraint;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.lock.definition.LockDefinition;

import java.util.List;
import java.util.Map;

public interface AllocationHelper {

    /**
     * This address various usability issues allowing users to provide shorthand versions of the service
     * name
     */
    void normalizeLabels(long stackId, Map<String, String> systemLabels, Map<String, String> serviceUserLabels);

    List<Long> getHostsSatisfyingHostAffinity(Long accountId, Map<String, String> labelConstraints);

    @SuppressWarnings("rawtypes")
    List<Constraint> extractConstraintsFromEnv(Map env);

    @SuppressWarnings("rawtypes")
    List<Constraint> extractConstraintsFromLabels(Map labels, Instance instance);

    List<Long> getAllHostsSatisfyingHostAffinity(Long accountId, Map<String, String> labelConstraints);

    List<LockDefinition> extractAllocationLockDefinitions(Instance instance);
}
