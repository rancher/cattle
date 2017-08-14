package io.cattle.platform.core.cache;

import java.util.HashSet;
import java.util.Set;

public class QueryOptions {

    Long clusterId;
    String kind;
    boolean includeUsedPorts;
    Set<Long> hosts = new HashSet<>();
    Long requestedHostId;

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public Set<Long> getHosts() {
        return hosts;
    }

    public Long getClusterId() {
        return clusterId;
    }

    public void setClusterId(Long clusterId) {
        this.clusterId = clusterId;
    }

    public void setHosts(Set<Long> hosts) {
        this.hosts = hosts;
    }

    public boolean isIncludeUsedPorts() {
        return includeUsedPorts;
    }

    public void setIncludeUsedPorts(boolean getUsedPorts) {
        this.includeUsedPorts = getUsedPorts;
    }

    public Long getRequestedHostId() {
        return requestedHostId;
    }

    public void setRequestedHostId(Long requestedHostId) {
        this.requestedHostId = requestedHostId;
    }

}
