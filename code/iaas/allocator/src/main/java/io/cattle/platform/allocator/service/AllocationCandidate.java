package io.cattle.platform.allocator.service;

import io.cattle.platform.core.model.Port;
import io.cattle.platform.object.ObjectManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

public class AllocationCandidate {

    String id = io.cattle.platform.util.resource.UUID.randomUUID().toString();
    Long host;
    String hostUuid;
    List<Port> usedPorts;
    ObjectManager objectManager;
    Map<Pair<Class<?>, Long>, Object> resources;

    public AllocationCandidate() {
    }

    public AllocationCandidate(AllocationCandidate candidate) {
        this.host = candidate.host;
        this.hostUuid = candidate.hostUuid;
        this.usedPorts = candidate.usedPorts == null ? new ArrayList<>() : new ArrayList<>(candidate.usedPorts);
        this.objectManager = candidate.objectManager;
        this.resources = candidate.resources == null ? null : new HashMap<>(candidate.resources);
    }

    public AllocationCandidate(ObjectManager objectManager, Map<Pair<Class<?>, Long>, Object> resources, Long hostId, String hostUuid, List<Port> usedPorts,
            Map<Long, Long> pools) {
        super();
        this.objectManager = objectManager;
        this.resources = resources;
        this.host = hostId;
        this.hostUuid = hostUuid;
        this.usedPorts = usedPorts;
    }

    @SuppressWarnings("unchecked")
    public <T> T loadResource(Class<T> clz, Long id) {
        if (id == null) {
            return null;
        }

        Pair<Class<?>, Long> key = new ImmutablePair<>(clz, id);
        Object resource = resources.get(key);

        if (resource == null) {
            resource = objectManager.loadResource(clz, id);
            resources.put(key, resource);
        }

        return (T) resource;
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

}