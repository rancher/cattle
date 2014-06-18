package io.cattle.platform.configitem.context.data;

import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.NetworkService;
import io.cattle.platform.core.model.Subnet;
import io.cattle.platform.core.model.Vnet;

public class HostRouteData {

    Instance instance;
    Subnet subnet;
    Vnet vnet;
    NetworkService hostNatGatewayService;

    public Subnet getSubnet() {
        return subnet;
    }

    public void setSubnet(Subnet subnet) {
        this.subnet = subnet;
    }

    public Instance getInstance() {
        return instance;
    }

    public void setInstance(Instance instance) {
        this.instance = instance;
    }

    public NetworkService getHostNatGatewayService() {
        return hostNatGatewayService;
    }

    public void setHostNatGatewayService(NetworkService hostNatGatewayService) {
        this.hostNatGatewayService = hostNatGatewayService;
    }

    public Vnet getVnet() {
        return vnet;
    }

    public void setVnet(Vnet vnet) {
        this.vnet = vnet;
    }

}
