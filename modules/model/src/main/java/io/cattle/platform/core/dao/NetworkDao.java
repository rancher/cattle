package io.cattle.platform.core.dao;

import io.cattle.platform.core.model.Network;
import io.cattle.platform.core.model.Subnet;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface NetworkDao {

    Network getNetworkByKind(long accountId, String kind);

    Network getNetworkByName(long accountId, String name);

    Network getDefaultNetwork(Long accountId);

    List<Subnet> getSubnets(Network network);

    List<Long> findInstancesInUseByServiceDriver(Collection<Long> id);

    Collection<? extends Network> getNetworks(Set<Long> networkIds);
}
