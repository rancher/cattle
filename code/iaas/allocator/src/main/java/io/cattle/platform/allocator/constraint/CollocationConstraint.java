package io.cattle.platform.allocator.constraint;

import io.cattle.platform.allocator.service.AllocationCandidate;

import java.util.Set;

import org.apache.commons.lang3.StringUtils;

public class CollocationConstraint extends HardConstraint implements Constraint {

    Long hostId;
    Set<Long> otherInstances;

    public CollocationConstraint(Long hostId, Set<Long> otherInstances) {
        this.hostId = hostId;
        this.otherInstances = otherInstances;
    }

    @Override
    public boolean matches(AllocationCandidate candidate) {
        return this.hostId.equals(candidate.getHost());
    }

    @Override
    public String toString() {
        return String.format("On the same host (%s) as %s", hostId, StringUtils.join(otherInstances, ", "));
    }

}
