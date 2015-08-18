package io.cattle.platform.allocator.dao;

import io.cattle.platform.allocator.service.AllocationAttempt;
import io.cattle.platform.allocator.service.AllocationCandidate;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Port;
import io.cattle.platform.core.model.StoragePool;
import io.cattle.platform.core.model.Volume;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Multimap;

public interface AllocatorDao {

    List<? extends StoragePool> getAssociatedPools(Volume volume);

    List<? extends StoragePool> getAssociatedPools(Host host);

    List<? extends Host> getHosts(Instance instance);

    List<? extends Host> getHosts(StoragePool pool);

    boolean recordCandidate(AllocationAttempt attempt, AllocationCandidate candidate);

    void releaseAllocation(Instance instance);

    void releaseAllocation(Volume volume);

    boolean isInstanceImageKind(long instanceId, String kind);

    boolean isVolumeInstanceImageKind(long volumeId, String kind);

    List<Long> getHostsForSubnet(long subnetId, Long vnetId);

    List<Long> getPhysicalHostsForSubnet(long subnetId, Long vnetId);

    List<Port> getUsedPortsForHostExcludingInstance(long hostId, long instanceId);

    // key -> [value,mapping.state]
    Map<String, String[]> getLabelsForHost(long hostId);

    boolean hostHasContainerLabel(long hostId, String labelKey, String labelValue);

    Multimap<String, String> getLabelsForContainersForHost(long hostId);

    List<? extends Host> getActiveHosts(long accountId);
}
