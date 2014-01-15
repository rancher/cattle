package io.github.ibuildthecloud.dstack.allocator.dao;

import io.github.ibuildthecloud.dstack.allocator.service.AllocationAttempt;
import io.github.ibuildthecloud.dstack.allocator.service.AllocationCandidate;
import io.github.ibuildthecloud.dstack.core.model.Host;
import io.github.ibuildthecloud.dstack.core.model.Instance;
import io.github.ibuildthecloud.dstack.core.model.StoragePool;
import io.github.ibuildthecloud.dstack.core.model.Volume;

import java.util.List;

public interface AllocatorDao {

    List<? extends StoragePool> getAssociatedPools(Volume volume);

    List<? extends Host> getHosts(Instance instance);

    List<? extends Host> getHosts(StoragePool pool);

    boolean recordCandidate(AllocationAttempt attempt, AllocationCandidate candidate);

    void releaseAllocation(Instance instance);

}
