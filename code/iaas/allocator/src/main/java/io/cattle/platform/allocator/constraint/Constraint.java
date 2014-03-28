package io.cattle.platform.allocator.constraint;

import io.cattle.platform.allocator.service.AllocationAttempt;
import io.cattle.platform.allocator.service.AllocationCandidate;

public interface Constraint {

    boolean matches(AllocationAttempt attempt, AllocationCandidate candidate);

}
