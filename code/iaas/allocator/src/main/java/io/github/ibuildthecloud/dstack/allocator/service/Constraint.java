package io.github.ibuildthecloud.dstack.allocator.service;

public interface Constraint {

    boolean matches(AllocationAttempt attempt, AllocationCandidate candidate);

}
