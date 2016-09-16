package io.cattle.platform.allocator.constraint;

import io.cattle.platform.allocator.service.AllocationCandidate;

public interface Constraint {

    boolean matches(AllocationCandidate candidate);

    boolean isHardConstraint();

}
