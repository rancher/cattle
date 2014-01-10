package io.github.ibuildthecloud.dstack.simple.allocator.dao;

import io.github.ibuildthecloud.dstack.allocator.service.AllocationCandidate;

import java.util.Iterator;
import java.util.List;

public interface SimpleAllocatorDao {

    boolean isInstance(long instanceId, String kind);

    boolean isVolume(long volumeId, String kind);

    Iterator<AllocationCandidate> iteratorHosts(List<Long> volumeIds, String kind);

    Iterator<AllocationCandidate> iteratorPools(List<Long> volumeIds, String kind);
}
