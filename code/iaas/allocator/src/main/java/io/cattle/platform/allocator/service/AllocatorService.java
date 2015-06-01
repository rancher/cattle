package io.cattle.platform.allocator.service;

import io.cattle.platform.allocator.constraint.Constraint;

import java.util.List;
import java.util.Map;

public interface AllocatorService {

    /**
     * Add labels from 'srcMap' to 'destMap'.  If key already exists in destMap, either
     * overwrite or merge depending on whether the key is an affinity rule or not
     */
    void mergeLabels(Map<String, String> srcMap, Map<String, String> destMap);

    List<Long> getHostsSatisfyingHostAffinity(Long accountId, Map<String, String> labelConstraints);

    boolean hostSatisfiesHostAffinity(long hostId, Map<String, String> labelConstraints);

    @SuppressWarnings("rawtypes")
    List<Constraint> extractConstraintsFromEnv(Map env);

    @SuppressWarnings("rawtypes")
    List<Constraint> extractConstraintsFromLabels(Map labels);

}
