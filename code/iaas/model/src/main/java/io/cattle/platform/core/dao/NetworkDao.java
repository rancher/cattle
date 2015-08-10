package io.cattle.platform.core.dao;

import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Network;
import io.cattle.platform.core.model.NetworkService;
import io.cattle.platform.core.model.NetworkServiceProvider;
import io.cattle.platform.core.model.NetworkServiceProviderInstanceMap;
import io.cattle.platform.core.model.Nic;
import io.cattle.platform.core.model.Subnet;

import java.util.List;

public interface NetworkDao {

    List<? extends NetworkService> getAgentInstanceNetworkService(long instanceId, String serviceKind);

    List<? extends NetworkService> getNetworkService(long instanceId, String serviceKind);

    Nic getPrimaryNic(long instanceId);

    List<? extends Network> getNetworksForAccount(long accountId, String kind);

    Network getNetworkForObject(Object object, String networkKind);

    void registerNspInstance(String providerKind, Instance instance, List<String> services);

    List<NetworkServiceProviderInstanceMap> findNspInstanceMaps(Instance instance);

    Instance getServiceProviderInstanceOnHostForNetwork(long networkId, String serviceKind, long hostId);

    Instance getServiceProviderInstanceOnHost(String serviceKind, long hostId);

    Subnet addVIPSubnet(long accountId);

    Subnet addManagedNetworkSubnet(Network network);
    
    NetworkServiceProvider createNsp(Network network, List<String> servicesKinds, String providerKind);

    String getVIPSubnetCidr();
}
