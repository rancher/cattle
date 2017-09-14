package io.cattle.platform.allocator.service;

import io.cattle.platform.allocator.constraint.Constraint;
import io.cattle.platform.core.addon.metadata.HostInfo;
import io.cattle.platform.core.cache.QueryOptions;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.lock.definition.LockDefinition;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

public interface AllocationHelper {

    List<Long> getHostsSatisfyingHostAffinity(long clusterId, Map<String, ?> labelConstraints);

    List<Constraint> extractConstraintsFromEnv(Map<String, ?> env);

    List<Constraint> extractConstraintsFromLabels(Map<String, ?> labels, Instance instance);

    List<Long> getAllHostsSatisfyingHostAffinity(long clusterId, Map<String, ?> labelConstraints);

    List<LockDefinition> extractAllocationLockDefinitions(Instance instance, List<Instance> instances);

    boolean hostHasContainerLabel(long clusterId, String hostUuid, String labelKey, String labelValue);

    Map<String, String> getLabelsForHost(long clusterId, String hostUuid);

    Iterator<HostInfo> iterateHosts(QueryOptions options, List<String> orderedHostUUIDs);
}
