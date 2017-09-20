package io.cattle.platform.allocator.service;

import io.cattle.platform.util.resource.UUID;

import java.util.HashSet;
import java.util.Set;

public class AllocationCandidate {

    String id = UUID.randomUUID().toString();
    Long clusterId;
    Long host;
    String hostUuid;
    Set<String> usedPorts;

    public AllocationCandidate(long clusterId, long hostId, String hostUuid) {
        this.clusterId = clusterId;
        this.host = hostId;
        this.hostUuid = hostUuid;
    }

    public AllocationCandidate(AllocationCandidate candidate) {
        this.clusterId = candidate.clusterId;
        this.host = candidate.host;
        this.hostUuid = candidate.hostUuid;
        this.usedPorts = candidate.usedPorts == null ? new HashSet<>() : new HashSet<>(candidate.usedPorts);
    }

    public AllocationCandidate(Long hostId, String hostUuid, Set<String> usedPorts, Long clusterId) {
        super();
        this.clusterId = clusterId;
        this.host = hostId;
        this.hostUuid = hostUuid;
        this.usedPorts = usedPorts;
    }

    public String getId() {
        return id;
    }

    public Long getHost() {
        return host;
    }

    public void setHost(Long host) {
        this.host = host;
    }

    public String getHostUuid() {
        return hostUuid;
    }

    public Set<String> getUsedPorts() {
        return usedPorts;
    }

    public Long getClusterId() {
        return clusterId;
    }

}