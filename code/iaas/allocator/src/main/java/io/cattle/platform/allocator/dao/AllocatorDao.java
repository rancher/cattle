package io.cattle.platform.allocator.dao;

import io.cattle.platform.allocator.dao.impl.QueryOptions;
import io.cattle.platform.allocator.service.AllocationAttempt;
import io.cattle.platform.allocator.service.AllocationCandidate;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.InstanceHostMap;
import io.cattle.platform.core.model.StoragePool;
import io.cattle.platform.core.model.Volume;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface AllocatorDao {

    Host getHost(Instance instance);

    List<? extends Host> getHosts(Collection<? extends StoragePool> storagePoolsIds);

    boolean recordCandidate(AllocationAttempt attempt, AllocationCandidate candidate);

    void releaseAllocation(Instance instance, InstanceHostMap map);

    boolean isVolumeInUseOnHost(long volumeId, long hostId);

    Set<Long> findHostsWithVolumeInUse(long volumeId);

    List<Long> getInstancesWithVolumeMounted(long volumeId, long currentInstanceId);

    Map<String, List<InstanceHostMap>> getInstanceHostMapsWithHostUuid(long instanceId);

    List<Instance> getUnmappedDeploymentUnitInstances(Long deploymentUnitId);

    boolean isAllocationReleased(Object resource);

    Iterator<AllocationCandidate> iteratorHosts(List<String> orderedHostUuids, List<Long> volumes, QueryOptions options);

    Long getHostAffinityForVolume(Volume volume);
 }
