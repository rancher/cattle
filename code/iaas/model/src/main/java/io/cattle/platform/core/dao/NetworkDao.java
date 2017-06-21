package io.cattle.platform.core.dao;

import io.cattle.platform.core.model.Network;
import io.cattle.platform.core.model.Nic;
import io.cattle.platform.core.model.Subnet;

import java.util.List;

public interface NetworkDao {

    List<? extends Network> findBadNetworks(int limit);

    Nic getPrimaryNic(long instanceId);

    Network getNetworkByKind(long accountId, String kind);

    Network getNetworkByName(long accountId, String name);

    Subnet addVIPSubnet(long accountId);

    Network getDefaultNetwork(Long accountId);

    List<? extends Network> getActiveNetworks(Long accountId);

    List<Subnet> getSubnets(Network network);

    List<Long> findInstancesInUseByServiceDriver(Long id);

    void migrateToNetwork(Network network);

}
