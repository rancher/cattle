package io.github.ibuildthecloud.dstack.simulator.allocator.dao;

import io.github.ibuildthecloud.dstack.allocator.service.AllocationCandidate;

import java.util.Iterator;
import java.util.List;

public interface SimulatorAllocatorDao {

    boolean isSimulatedInstance(long instanceId);

    boolean isSimulatedVolume(long volumeId);

    Iterator<AllocationCandidate> iteratorHosts(List<Long> volumeIds);

    Iterator<AllocationCandidate> iteratorPools(List<Long> volumeIds);
}
