package io.cattle.platform.allocator.service;

import io.cattle.platform.core.model.Port;
import io.cattle.platform.util.resource.UUID;

import java.util.ArrayList;
import java.util.List;

public class AllocationCandidate {

    String id = UUID.randomUUID().toString();
    Long accountId;
    Long host;
    String hostUuid;
    List<Port> usedPorts;

    public AllocationCandidate() {
    }

    public AllocationCandidate(AllocationCandidate candidate) {
        this.accountId = candidate.accountId;
        this.host = candidate.host;
        this.hostUuid = candidate.hostUuid;
        this.usedPorts = candidate.usedPorts == null ? new ArrayList<>() : new ArrayList<>(candidate.usedPorts);
    }

    public AllocationCandidate(Long hostId, String hostUuid, List<Port> usedPorts, Long accountId) {
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

    public List<Port> getUsedPorts() {
        return usedPorts;
    }

    public Long getAccountId() {
        return accountId;
    }

}