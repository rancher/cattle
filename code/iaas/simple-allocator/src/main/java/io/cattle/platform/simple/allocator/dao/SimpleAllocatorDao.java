package io.cattle.platform.simple.allocator.dao;

import io.cattle.platform.allocator.service.AllocationCandidate;
import io.cattle.platform.simple.allocator.AllocationCandidateCallback;

import java.util.Iterator;
import java.util.List;

public interface SimpleAllocatorDao {

    Iterator<AllocationCandidate> iteratorHosts(List<Long> volumeIds, QueryOptions options, AllocationCandidateCallback callback);

    Iterator<AllocationCandidate> iteratorPools(List<Long> volumeIds, QueryOptions options);

}
