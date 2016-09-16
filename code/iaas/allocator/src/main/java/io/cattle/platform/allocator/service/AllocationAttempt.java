package io.cattle.platform.allocator.service;

import io.cattle.platform.allocator.constraint.Constraint;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Nic;
import io.cattle.platform.core.model.StoragePool;
import io.cattle.platform.core.model.Subnet;
import io.cattle.platform.core.model.Volume;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class AllocationAttempt {

    public enum AllocationType {
        INSTANCE, VOLUME
    }

    String id = UUID.randomUUID().toString();

    AllocationType type;

    Long accountId;

    List<Instance> instances;

    Long hostId;

    Long requestedHostId;

    Set<Long> volumeIds;

    Map<Volume, Set<StoragePool>> pools;
    Map<Long, Set<Long>> poolIds;

    Set<Nic> nics;
    Set<Long> nicIds;

    Map<Nic, Subnet> subnets;
    Map<Long, Long> subnetIds;

    List<Constraint> constraints = new ArrayList<Constraint>();
    List<AllocationCandidate> candidates = new ArrayList<AllocationCandidate>();
    AllocationCandidate matchedCandidate;

    public AllocationAttempt(AllocationType type, long accountId, List<Instance> instances, Long hostId, Long requestedHostId, Set<Volume> volumes,
            Map<Volume, Set<StoragePool>> pools, Set<Nic> nics, Map<Nic, Subnet> subnets) {
        super();
        this.type = type;
        this.accountId = accountId;
        this.instances = instances;
        this.hostId = hostId;
        this.pools = pools;
        this.nics = nics;
        this.subnets = subnets == null ? Collections.<Nic, Subnet> emptyMap() : subnets;

        this.volumeIds = new HashSet<Long>(volumes.size());
        this.poolIds = new HashMap<Long, Set<Long>>();
        for (Volume v : volumes) {
            this.volumeIds.add(v.getId());
            Set<StoragePool> storagePools = pools.get(v);

            if (storagePools != null) {
                Set<Long> poolIds = new HashSet<Long>(storagePools.size());
                for (StoragePool pool : storagePools) {
                    poolIds.add(pool.getId());
                }
                this.poolIds.put(v.getId(), poolIds);
            }
        }

        if (nics == null) {
            this.nics = Collections.emptySet();
            this.nicIds = Collections.emptySet();
            this.subnetIds = Collections.emptyMap();
        } else {
            this.nicIds = new HashSet<Long>(subnets.size());
            this.subnetIds = new HashMap<Long, Long>();
            for (Nic n : nics) {
                this.nicIds.add(n.getId());

                Subnet subnet = this.subnets.get(n);
                if (subnet != null) {
                    this.subnetIds.put(n.getId(), subnet.getId());
                }
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

    public Set<Long> getVolumeIds() {
        return volumeIds;
    }

    public void setVolumeIds(Set<Long> volumeId) {
        this.volumeIds = volumeId;
    }

    public Map<Long, Set<Long>> getPoolIds() {
        return poolIds;
    }

    public void setPoolIds(Map<Long, Set<Long>> poolIds) {
        this.poolIds = poolIds;
    }

    public Set<Nic> getNics() {
        return nics;
    }

    public void setNics(Set<Nic> nics) {
        this.nics = nics;
    }

    public Set<Long> getNicIds() {
        return nicIds;
    }

    public void setNicIds(Set<Long> nicIds) {
        this.nicIds = nicIds;
    }

    public Map<Nic, Subnet> getSubnets() {
        return subnets;
    }

    public void setSubnets(Map<Nic, Subnet> subnets) {
        this.subnets = subnets;
    }

    public Map<Long, Long> getSubnetIds() {
        return subnetIds;
    }

    public void setSubnetIds(Map<Long, Long> subnetIds) {
        this.subnetIds = subnetIds;
    }
}
