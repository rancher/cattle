package io.cattle.platform.core.dao;

import io.cattle.platform.core.model.IpAddress;
import io.cattle.platform.core.model.Network;
import io.cattle.platform.core.model.Nic;
import io.cattle.platform.core.model.Subnet;

import java.util.List;
import java.util.Map;

public interface NetworkDao {

    Nic getPrimaryNic(long instanceId);

    Network getNetworkByKind(long accountId, String kind);

    Network getNetworkByName(long accountId, String name);

    Subnet addVIPSubnet(long accountId);

    Map<Long, IpAddress> getInstanceWithHostNetworkingToIpMap(long accountId);

    Network getDefaultNetwork(Long accountId);

    List<? extends Network> getActiveNetworks(Long accountId);

    List<Subnet> getSubnets(Network network);

    List<Long> findInstancesInUseByServiceDriver(Long id);

}
