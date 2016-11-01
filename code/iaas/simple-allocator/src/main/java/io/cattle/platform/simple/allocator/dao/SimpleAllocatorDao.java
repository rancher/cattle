package io.cattle.platform.simple.allocator.dao;

import io.cattle.platform.allocator.service.AllocationCandidate;

import java.util.Iterator;
import java.util.List;

public interface SimpleAllocatorDao {

    Iterator<AllocationCandidate> iteratorHosts(List<String> hostUUIDs, List<Long> volumeIds, QueryOptions options);

    Iterator<AllocationCandidate> iteratorPools(List<Long> volumeIds, QueryOptions options);

}
