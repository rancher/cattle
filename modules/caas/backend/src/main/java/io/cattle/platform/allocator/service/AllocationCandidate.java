package io.cattle.platform.allocator.service;

import io.cattle.platform.core.addon.PortInstance;
import io.cattle.platform.util.resource.UUID;

import java.util.HashSet;
import java.util.Set;

public class AllocationCandidate {

    String id = UUID.randomUUID().toString();
    Long accountId;
    Long host;
    String hostUuid;
    Set<PortInstance> usedPorts;

    public AllocationCandidate(long accountId, long hostId, String hostUuid) {
        this.accountId = accountId;
        this.host = hostId;
        this.hostUuid = hostUuid;
    }

    public AllocationCandidate(AllocationCandidate candidate) {
        this.accountId = candidate.accountId;
        this.host = candidate.host;
        this.hostUuid = candidate.hostUuid;
        this.usedPorts = candidate.usedPorts == null ? new HashSet<>() : new HashSet<>(candidate.usedPorts);
    }

    public AllocationCandidate(Long hostId, String hostUuid, Set<PortInstance> usedPorts, Long accountId) {
        super();
        this.accountId = accountId;
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

    public Set<PortInstance> getUsedPorts() {
        return usedPorts;
    }

    public Long getAccountId() {
        return accountId;
    }

}