package io.github.ibuildthecloud.dstack.allocator.constraint;

import io.github.ibuildthecloud.dstack.allocator.service.AllocationAttempt;
import io.github.ibuildthecloud.dstack.allocator.service.AllocationCandidate;

public interface Constraint {

    boolean matches(AllocationAttempt attempt, AllocationCandidate candidate);

}
