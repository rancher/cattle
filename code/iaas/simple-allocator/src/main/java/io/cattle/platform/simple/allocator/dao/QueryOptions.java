package io.cattle.platform.simple.allocator.dao;

import java.util.HashSet;
import java.util.Set;

public class QueryOptions {

    String kind;
    Long compute;
    Set<Long> hosts = new HashSet<Long>();

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public Long getCompute() {
        return compute;
    }

    public void setCompute(Long compute) {
        this.compute = compute;
    }

    public Set<Long> getHosts() {
        return hosts;
    }

}
