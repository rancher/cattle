package io.cattle.platform.configitem.context.dao;

import io.cattle.platform.configitem.context.data.ClientIpsecTunnelInfo;
import io.cattle.platform.configitem.context.data.HostInstanceData;
import io.cattle.platform.configitem.context.data.HostPortForwardData;
import io.cattle.platform.configitem.context.data.HostRouteData;
import io.cattle.platform.configitem.context.data.NetworkClientData;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Network;
import io.cattle.platform.core.model.NetworkService;
import io.cattle.platform.core.model.Nic;
import io.cattle.platform.core.model.Subnet;

import java.util.List;
import java.util.Map;

public interface NetworkInfoDao {

    List<NetworkClientData> networkClients(Instance instance);

    List<NetworkClientData> vnetClients(Instance instance);

    List<? extends NetworkService> networkServices(Instance instance);

    Map<Long, Network> networks(Instance instance);

    List<ClientIpsecTunnelInfo> getIpsecTunnelClient(Instance instance);

    List<HostPortForwardData> getHostPorts(Agent agent);

    List<HostRouteData> getHostRoutes(Agent agent);

    Map<Nic, Subnet> getNicsAndSubnet(Instance instance);

    List<? extends NetworkService> networkServices(Agent agent);

    List<HostInstanceData> getHostInstances(Agent agent);

}
