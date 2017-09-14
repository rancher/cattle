package io.cattle.platform.allocator.constraint;

import io.cattle.platform.allocator.service.AllocationCandidate;

public class ClusterConstraint extends HardConstraint implements Constraint {
    long clusterId;

    public ClusterConstraint(long clusterId) {
        this.clusterId = clusterId;
    }

    @Override
    public boolean matches(AllocationCandidate candidate) {
        // This constraint and doesn't need to  do any matching since the query will limit allocation based on cluster id.
        return true;
    }

    @Override
    public String toString() {
        return String.format("cluster id must be %d", clusterId);
    }
}
