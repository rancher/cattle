package io.cattle.platform.allocator.service;

import io.cattle.platform.allocator.constraint.Constraint;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.lock.definition.LockDefinition;

import java.util.List;
import java.util.Map;

public interface AllocationHelper {

    List<Long> getHostsSatisfyingHostAffinity(long accountId, Map<String, ?> labelConstraints);

    List<Constraint> extractConstraintsFromEnv(Map<String, ?> env);

    List<Constraint> extractConstraintsFromLabels(Map<String, ?> labels, Instance instance);

    List<Long> getAllHostsSatisfyingHostAffinity(long accountId, Map<String, ?> labelConstraints);

    List<LockDefinition> extractAllocationLockDefinitions(Instance instance, List<Instance> instances);
}
