package io.cattle.platform.allocator.dao.impl;

import java.util.HashSet;
import java.util.Set;

public class QueryOptions {

    Long accountId;
    String kind;
    boolean includeUsedPorts;
    Set<Long> hosts = new HashSet<Long>();
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

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
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
