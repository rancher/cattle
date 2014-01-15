package io.github.ibuildthecloud.dstack.allocator.service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class AllocationCandidate {

    String id = UUID.randomUUID().toString();
    Set<Long> hosts = new HashSet<Long>();
    Map<Long, Set<Long>> pools = new HashMap<Long, Set<Long>>();

    public AllocationCandidate() {
    }

    public AllocationCandidate(Long hostId, Map<Long, Long> pools) {
        super();
        this.hosts = new HashSet<Long>();
        if ( hostId != null ) {
            this.hosts.add(hostId);
        }
        this.pools = new HashMap<Long, Set<Long>>();

        for ( Map.Entry<Long, Long> entry : pools.entrySet() ) {
            Set<Long> set = new HashSet<Long>();
            set.add(entry.getValue());
            this.pools.put(entry.getKey(), set);
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Set<Long> getHosts() {
        return hosts;
    }

    public void setHosts(Set<Long> hosts) {
        this.hosts = hosts;
    }

    public Map<Long, Set<Long>> getPools() {
        return pools;
    }

    public void setPools(Map<Long, Set<Long>> pools) {
        this.pools = pools;
    }

}
