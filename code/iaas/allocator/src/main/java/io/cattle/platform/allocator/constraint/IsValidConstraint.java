package io.cattle.platform.allocator.constraint;

import io.cattle.platform.allocator.service.AllocationAttempt;
import io.cattle.platform.allocator.service.AllocationCandidate;

public class IsValidConstraint implements Constraint {

    @Override
    public boolean matches(AllocationAttempt attempt, AllocationCandidate candidate) {
        return candidate.isValid();
    }

    @Override
    public String toString() {
        return "is valid";
    }

}
