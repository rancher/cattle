package io.cattle.platform.allocator.service;

import io.cattle.platform.object.ObjectManager;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

public class AllocationCandidate {

    String id = UUID.randomUUID().toString();
    Set<Long> hosts = new HashSet<Long>();
    Map<Long, Long> subnetIds = new HashMap<Long, Long>();
    Map<Long, Set<Long>> pools = new HashMap<Long, Set<Long>>();
    ObjectManager objectManager;
    Map<Pair<Class<?>, Long>, Object> resources;
    boolean valid = true;

    public AllocationCandidate() {
    }

    public AllocationCandidate(AllocationCandidate candidate) {
        this.hosts = candidate.hosts == null ? null : new HashSet<Long>(candidate.hosts);
        this.subnetIds = candidate.subnetIds == null ? null : new HashMap<Long, Long>(candidate.subnetIds);
        this.pools = candidate.pools == null ? null : new HashMap<Long, Set<Long>>(candidate.pools);
        this.objectManager = candidate.objectManager;
        this.resources = candidate.resources == null ? null : new HashMap<Pair<Class<?>,Long>, Object>(candidate.resources);
        this.valid = candidate.valid;
    }

    public AllocationCandidate(ObjectManager objectManager, Map<Pair<Class<?>, Long>, Object> resources, Long hostId, Map<Long, Long> pools) {
        super();
        this.objectManager = objectManager;
        this.resources = resources;
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

    @SuppressWarnings("unchecked")
    public <T> T loadResource(Class<T> clz, Long id) {
        if ( id == null ) {
            return null;
        }

        Pair<Class<?>,Long> key = new ImmutablePair<Class<?>, Long>(clz, id);
        Object resource = resources.get(key);

        if ( resource == null ) {
            resource = objectManager.loadResource(clz, id);
            resources.put(key, resource);
        }

        return (T)resource;
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

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public Map<Long, Long> getSubnetIds() {
        return subnetIds;
    }

    public void setSubnetIds(Map<Long, Long> subnetIds) {
        this.subnetIds = subnetIds;
    }

}