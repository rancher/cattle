package io.cattle.platform.core.dao;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.IpAddress;
import io.cattle.platform.core.model.PhysicalHost;
import io.github.ibuildthecloud.gdapi.id.IdFormatter;

import java.util.List;
import java.util.Map;

import com.netflix.config.DynamicLongProperty;

public interface HostDao {

    public final static DynamicLongProperty HOST_REMOVE_DELAY = ArchaiusUtil.getLong("host.remove.delay.seconds");

    List<? extends Host> getHosts(Long accountId, List<String> uuids);

    boolean hasActiveHosts(Long accountId);

    Host getHostForIpAddress(long ipAddressId);

    IpAddress getIpAddressForHost(Long hostId);

    Map<Long, List<Object>> getInstancesPerHost(List<Long> hosts, IdFormatter idFormatter);

    PhysicalHost createMachineForHost(Host host);

    Map<Long, PhysicalHost> getPhysicalHostsForHosts(List<Long> hosts);

    void updateNullUpdatedHosts();

    List<? extends Host> findHostsRemove();

}
