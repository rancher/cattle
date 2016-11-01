package io.cattle.platform.allocator.service;

import io.cattle.platform.allocator.constraint.Constraint;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.StoragePool;
import io.cattle.platform.core.model.Volume;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AllocationAttempt {

    public enum AllocationType {
        INSTANCE, VOLUME
    }

    String id = io.cattle.platform.util.resource.UUID.randomUUID().toString();

    AllocationType type;

    Long accountId;

    List<Instance> instances;

    Long hostId;

    Long requestedHostId;

    Set<Volume> volumes;

    Map<Volume, Set<StoragePool>> pools;
    Map<Long, Set<Long>> poolIds;

    List<Constraint> constraints = new ArrayList<Constraint>();
    List<AllocationCandidate> candidates = new ArrayList<AllocationCandidate>();
    AllocationCandidate matchedCandidate;

    public AllocationAttempt(AllocationType type, long accountId, List<Instance> instances, Long hostId, Long requestedHostId, Set<Volume> volumes,
            Map<Volume, Set<StoragePool>> pools) {
        super();
        this.type = type;
        this.accountId = accountId;
        this.instances = instances;
        this.hostId = hostId;
        this.requestedHostId = requestedHostId;
        this.pools = pools;
        this.volumes = volumes == null ? new HashSet<Volume>(): volumes;

        this.poolIds = new HashMap<Long, Set<Long>>();
        for (Volume v : volumes) {
            Set<StoragePool> storagePools = pools.get(v);

            if (storagePools != null) {
                Set<Long> poolIds = new HashSet<Long>(storagePools.size());
                for (StoragePool pool : storagePools) {
                    poolIds.add(pool.getId());
                }
                this.poolIds.put(v.getId(), poolIds);
            }
        }
    }

    public boolean isInstanceAllocation() {
        return AllocationType.INSTANCE.equals(type);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public AllocationType getType() {
        return type;
    }

    public void setType(AllocationType type) {
        this.type = type;
    }

    public Long getAccountId() {
        return accountId;
    }

    public List<Instance> getInstances() {
        return instances;
    }

    public void setInstances(List<Instance> instances) {
        this.instances = instances;
    }

    public Long getHostId() {
        return hostId;
    }

    public void setHostId(Long hostId) {
        this.hostId = hostId;
    }

    public Long getRequestedHostId() {
        return requestedHostId;
    }

    public void setRequestedHostId(Long requestedHostId) {
        this.requestedHostId = requestedHostId;
    }

    public Map<Volume, Set<StoragePool>> getPools() {
        return pools;
    }

    public void setPools(Map<Volume, Set<StoragePool>> pools) {
        this.pools = pools;
    }

    public List<Constraint> getConstraints() {
        return constraints;
    }

    public void setConstraints(List<Constraint> constraints) {
        this.constraints = constraints;
    }

    public List<AllocationCandidate> getCandidates() {
        return candidates;
    }

    public void setCandidates(List<AllocationCandidate> candidates) {
        this.candidates = candidates;
    }

    public AllocationCandidate getMatchedCandidate() {
        return matchedCandidate;
    }

    public void setMatchedCandidate(AllocationCandidate matchedCandidate) {
        this.matchedCandidate = matchedCandidate;
    }

    public Set<Volume> getVolumes() {
        return volumes;
    }

    public Map<Long, Set<Long>> getPoolIds() {
        return poolIds;
    }

    public void setPoolIds(Map<Long, Set<Long>> poolIds) {
        this.poolIds = poolIds;
    }

}
