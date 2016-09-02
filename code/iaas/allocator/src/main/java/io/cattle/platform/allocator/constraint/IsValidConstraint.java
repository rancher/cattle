package io.cattle.platform.allocator.constraint;

import io.cattle.platform.allocator.service.AllocationCandidate;

public class IsValidConstraint extends HardConstraint implements Constraint {

    @Override
    public boolean matches(AllocationCandidate candidate) {
        return candidate.isValid();
    }

    @Override
    public String toString() {
        return "is valid";
    }

}
