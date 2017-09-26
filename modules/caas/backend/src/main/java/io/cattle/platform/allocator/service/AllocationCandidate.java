package io.cattle.platform.allocator.service;

import io.cattle.platform.util.resource.UUID;

public class AllocationCandidate {

    String id = UUID.randomUUID().toString();
    Long clusterId;
    Long host;
    String hostUuid;

    public AllocationCandidate(long clusterId, long hostId, String hostUuid) {
        this.clusterId = clusterId;
        this.host = hostId;
        this.hostUuid = hostUuid;
    }

    public AllocationCandidate(AllocationCandidate candidate) {
        this.clusterId = candidate.clusterId;
        this.host = candidate.host;
        this.hostUuid = candidate.hostUuid;
    }

    public AllocationCandidate(Long hostId, String hostUuid, Long clusterId) {
        super();
        this.clusterId = clusterId;
        this.host = hostId;
        this.hostUuid = hostUuid;
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

    public Long getClusterId() {
        return clusterId;
    }

}