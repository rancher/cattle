package io.cattle.platform.allocator.dao;

import io.cattle.platform.allocator.service.AllocationAttempt;
import io.cattle.platform.allocator.service.AllocationCandidate;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.InstanceHostMap;
import io.cattle.platform.core.model.Port;
import io.cattle.platform.core.model.StoragePool;
import io.cattle.platform.core.model.Volume;

import java.util.List;
import java.util.Map;

public interface AllocatorDao {

    List<? extends StoragePool> getAssociatedPools(Volume volume);

    List<? extends StoragePool> getAssociatedUnmanagedPools(Host host);

    Host getHost(Instance instance);

    List<? extends Host> getHosts(StoragePool pool);

    boolean recordCandidate(AllocationAttempt attempt, AllocationCandidate candidate);

    void releaseAllocation(Instance instance, InstanceHostMap map);

    void releaseAllocation(Volume volume);

    boolean isInstanceImageKind(long instanceId, String kind);

    boolean isVolumeInstanceImageKind(long volumeId, String kind);

    List<Port> getUsedPortsForHostExcludingInstance(long hostId, long instanceId);

    // key -> [value,mapping.state]
    Map<String, String[]> getLabelsForHost(long hostId);

    boolean hostHasContainerLabel(long hostId, String labelKey, String labelValue);

    List<? extends Host> getActiveHosts(long accountId);

    List<? extends Host> getNonPurgedHosts(long accountId);

    boolean isVolumeInUseOnHost(long volumeId, long hostId);

    List<Long> getInstancesWithVolumeMounted(long volumeId, long currentInstanceId);

    Map<String, List<InstanceHostMap>> getInstanceHostMapsWithHostUuid(long instanceId);

    List<Instance> getUnmappedDeploymentUnitInstances(String deploymentUnitUuid);

    boolean isAllocationReleased(Object resource);

    String getAllocatedHostUuid(Volume volume);
 }
