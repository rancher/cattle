package io.cattle.platform.configitem.context.data;

import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.IpAddress;
import io.cattle.platform.core.model.Subnet;

public class ClientIpsecTunnelInfo {

    Instance instance;
    Instance agentInstance;
    Host host;
    IpAddress hostIpAddress;
    IpAddress ipAddress;
    Subnet subnet;
    int natPort;
    int isaKmpPort;

    public Instance getInstance() {
        return instance;
    }

    public void setInstance(Instance instance) {
        this.instance = instance;
    }

    public Instance getAgentInstance() {
        return agentInstance;
    }

    public void setAgentInstance(Instance agentInstance) {
        this.agentInstance = agentInstance;
    }

    public Host getHost() {
        return host;
    }

    public void setHost(Host host) {
        this.host = host;
    }

    public IpAddress getHostIpAddress() {
        return hostIpAddress;
    }

    public void setHostIpAddress(IpAddress hostIpAddress) {
        this.hostIpAddress = hostIpAddress;
    }

    public int getNatPort() {
        return natPort;
    }

    public void setNatPort(int natPort) {
        this.natPort = natPort;
    }

    public int getIsaKmpPort() {
        return isaKmpPort;
    }

    public void setIsaKmpPort(int isaKmpPort) {
        this.isaKmpPort = isaKmpPort;
    }

    public IpAddress getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(IpAddress ipAddress) {
        this.ipAddress = ipAddress;
    }

    public Subnet getSubnet() {
        return subnet;
    }

    public void setSubnet(Subnet subnet) {
        this.subnet = subnet;
    }

}
