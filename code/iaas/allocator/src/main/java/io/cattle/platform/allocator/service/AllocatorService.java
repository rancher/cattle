package io.cattle.platform.allocator.service;

import io.cattle.platform.allocator.constraint.Constraint;

import java.util.List;
import java.util.Map;

public interface AllocatorService {

    public List<Long> getHostsForGlobalService(Long accountId, Map<String, String> labels);

    @SuppressWarnings("rawtypes")
    public List<Constraint> extractConstraintsFromEnv(Map env);

    @SuppressWarnings("rawtypes")
    public List<Constraint> extractConstraintsFromLabels(Map labels);

}
