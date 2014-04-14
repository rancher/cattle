package io.cattle.platform.allocator.constraint;

import java.util.HashSet;
import java.util.Set;

import io.cattle.platform.allocator.service.AllocationAttempt;
import io.cattle.platform.allocator.service.AllocationCandidate;

public class ValidSubnetsConstraint implements Constraint {

    long nicId;
    Set<Long> subnets = new HashSet<Long>();

    public ValidSubnetsConstraint(long nicId) {
        super();
        this.nicId = nicId;
    }

    public void addSubnet(Long subnetId) {
        subnets.add(subnetId);
    }

    @Override
    public boolean matches(AllocationAttempt attempt, AllocationCandidate candidate) {
        if ( subnets.size() == 0 ) {
            return true;
        }

        Long subnetId = candidate.getSubnetIds().get(nicId);
        if ( subnetId == null ) {
            return false;
        }

        return subnets.contains(subnetId);
    }

    @Override
    public String toString() {
        return String.format("nic [%s] valid subnets %s", nicId, subnets);
    }

    public long getNicId() {
        return nicId;
    }

    public Set<Long> getSubnets() {
        return subnets;
    }

}