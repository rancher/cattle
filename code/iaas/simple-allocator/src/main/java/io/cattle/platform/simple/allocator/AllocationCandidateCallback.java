package io.cattle.platform.simple.allocator;

import io.cattle.platform.allocator.service.AllocationCandidate;

import java.util.List;

public interface AllocationCandidateCallback {

    List<AllocationCandidate> withCandidate(AllocationCandidate candidate);

}
