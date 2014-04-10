package io.cattle.platform.simple.allocator;

import java.util.List;

import io.cattle.platform.allocator.service.AllocationCandidate;

public interface AllocationCandidateCallback {

    List<AllocationCandidate> withCandidate(AllocationCandidate candidate);

}
