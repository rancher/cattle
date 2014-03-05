package io.github.ibuildthecloud.dstack.simple.allocator.dao;

import io.github.ibuildthecloud.dstack.allocator.service.AllocationCandidate;

import java.util.Iterator;
import java.util.List;

public interface SimpleAllocatorDao {

    Iterator<AllocationCandidate> iteratorHosts(List<Long> volumeIds, QueryOptions options);

    Iterator<AllocationCandidate> iteratorPools(List<Long> volumeIds, QueryOptions options);

}
