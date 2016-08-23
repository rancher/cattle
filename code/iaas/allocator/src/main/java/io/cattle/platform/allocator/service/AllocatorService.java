package io.cattle.platform.allocator.service;

import io.cattle.platform.allocator.constraint.Constraint;
import io.cattle.platform.core.model.Instance;

import java.util.List;
import java.util.Map;

public interface AllocatorService {

    /**
     * Add labels from 'srcMap' to 'destMap'.  If key already exists in destMap, either
     * overwrite or merge depending on whether the key is an affinity rule or not
     */
    void mergeLabels(Map<String, String> srcMap, Map<String, String> destMap);

    /**
     * This address various usability issues allowing users to provide shorthand versions of the service
     * name
     */
    void normalizeLabels(long stackId, Map<String, String> systemLabels, Map<String, String> serviceUserLabels);

    List<Long> getHostsSatisfyingHostAffinity(Long accountId, Map<String, String> labelConstraints);

    boolean hostChangesAffectsHostAffinityRules(long hostId, Map<String, String> labelConstraints);

    @SuppressWarnings("rawtypes")
    List<Constraint> extractConstraintsFromEnv(Map env);

    @SuppressWarnings("rawtypes")
    List<Constraint> extractConstraintsFromLabels(Map labels, Instance instance);

    List<Long> getAllHostsSatisfyingHostAffinity(Long accountId, Map<String, String> labelConstraints);

}
