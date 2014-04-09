package io.cattle.platform.networking.host.dao;

import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Network;
import io.cattle.platform.core.model.Subnet;
import io.cattle.platform.core.model.Vnet;

public interface HostOnlyDao {

    Vnet getVnetForHost(Network network, Host host);

    Vnet createVnetForHost(Network network, Host host, Subnet subnets, String uri);

}
