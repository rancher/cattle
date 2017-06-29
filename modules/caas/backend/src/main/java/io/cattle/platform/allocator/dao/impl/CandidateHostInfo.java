package io.cattle.platform.allocator.dao.impl;

import io.cattle.platform.core.model.Port;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CandidateHostInfo {
    private Long hostId;
    private String hostUuid;
    private Set<Long> poolIds = new HashSet<>();
    private List<Port> usedPorts = new ArrayList<>();
    private Map<String, Set<String>> containerLabels = new HashMap<>();
    
    
    public CandidateHostInfo(Long hostId, String hostUuid) {
        this.hostId = hostId;
        this.hostUuid = hostUuid;
    }

    public CandidateHostInfo(Long hostId, String hostUuid, Set<Long> poolIds, List<Port> usedPorts, Map<String, Set<String>> containerLabels) {
        this.hostId = hostId;
        this.hostUuid = hostUuid;
        this.poolIds = poolIds;
        this.usedPorts = usedPorts;
        this.containerLabels = containerLabels;
    }
    
    public Long getHostId() {
        return hostId;
    }
    public void setHostId(Long hostId) {
        this.hostId = hostId;
    }
    public Set<Long> getPoolIds() {
        return poolIds;
    }
    public void setPoolIds(Set<Long> poolIds) {
        this.poolIds = poolIds;
    }
    public String getHostUuid() {
        return hostUuid;
    }
    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }
    public List<Port> getUsedPorts() {
        return usedPorts;
    }
    public void setUsedPorts(List<Port> usedPorts) {
        this.usedPorts = usedPorts;
    }
    public Map<String, Set<String>> getContainerLabels() {
        return containerLabels;
    }
    public void setContainerLabels(Map<String, Set<String>> containerLabels) {
        this.containerLabels = containerLabels;
    }
    
    
    
    
}
