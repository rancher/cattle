package io.cattle.platform.allocator.service;

import io.cattle.platform.allocator.constraint.Constraint;
import io.cattle.platform.allocator.service.impl.ResourceRequest;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Volume;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AllocationAttempt {
    String id = io.cattle.platform.util.resource.UUID.randomUUID().toString();

    Long accountId;

    List<Instance> instances;

    Long hostId;

    Long requestedHostId;

    Set<Volume> volumes;

    List<Map<String, Object>> allocatedIPs;

    List<Constraint> constraints = new ArrayList<>();
    List<AllocationCandidate> candidates = new ArrayList<>();
    AllocationCandidate matchedCandidate;
    List<ResourceRequest> resourceRequests;

    public AllocationAttempt(long accountId, List<Instance> instances, Long hostId, Long requestedHostId, Set<Volume> volumes) {
        super();
        this.accountId = accountId;
        this.instances = instances;
        this.hostId = hostId;
        this.requestedHostId = requestedHostId;
        this.volumes = volumes == null ? new HashSet<>(): volumes;
    }

    public String getId() {
        return id;
    }

    public Long getAccountId() {
        return accountId;
    }

    public List<Instance> getInstances() {
        return instances;
    }

    public Long getHostId() {
        return hostId;
    }

    public Long getRequestedHostId() {
        return requestedHostId;
    }

    public List<Constraint> getConstraints() {
        return constraints;
    }

    public List<AllocationCandidate> getCandidates() {
        return candidates;
    }

    public AllocationCandidate getMatchedCandidate() {
        return matchedCandidate;
    }

    public void setMatchedCandidate(AllocationCandidate matchedCandidate) {
        this.matchedCandidate = matchedCandidate;
    }

    public Set<Volume> getVolumes() {
        return volumes;
    }

    public List<Map<String, Object>> getAllocatedIPs() {
        return allocatedIPs;
    }

    public void setAllocatedIPs(List<Map<String, Object>> allocatedIPs) {
        this.allocatedIPs = allocatedIPs;
    }

    public List<ResourceRequest> getResourceRequests() {
        return resourceRequests;
    }

    public void setResourceRequests(List<ResourceRequest> resourceRequests) {
        this.resourceRequests = resourceRequests;
    }
}
