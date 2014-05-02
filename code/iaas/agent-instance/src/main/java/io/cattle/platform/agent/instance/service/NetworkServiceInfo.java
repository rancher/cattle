package io.cattle.platform.agent.instance.service;

import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.IpAddress;
import io.cattle.platform.core.model.NetworkService;
import io.cattle.platform.core.model.NetworkServiceProvider;
import io.cattle.platform.core.model.Nic;

public class NetworkServiceInfo {

    NetworkServiceProvider networkServiceProvider;
    NetworkService networkService;
    Nic clientNic;
    Nic agentNic;
    Instance agentInstance;
    IpAddress ipAddress;

    public NetworkServiceInfo(NetworkServiceProvider networkServiceProvider, NetworkService networkService, Nic clientNic,
            Nic agentNic, IpAddress ipAddress, Instance agentInstance) {
        super();
        this.networkServiceProvider = networkServiceProvider;
        this.networkService = networkService;
        this.clientNic = clientNic;
        this.agentNic = agentNic;
        this.agentInstance = agentInstance;
        this.ipAddress = ipAddress;
    }

    public NetworkServiceProvider getNetworkServiceProvider() {
        return networkServiceProvider;
    }

    public void setNetworkServiceProvider(NetworkServiceProvider networkServiceProvider) {
        this.networkServiceProvider = networkServiceProvider;
    }

    public NetworkService getNetworkService() {
        return networkService;
    }

    public void setNetworkService(NetworkService networkService) {
        this.networkService = networkService;
    }

    public Instance getAgentInstance() {
        return agentInstance;
    }

    public void setAgentInstance(Instance agentInstance) {
        this.agentInstance = agentInstance;
    }

    public IpAddress getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(IpAddress ipAddress) {
        this.ipAddress = ipAddress;
    }

    public Nic getClientNic() {
        return clientNic;
    }

    public void setClientNic(Nic clientNic) {
        this.clientNic = clientNic;
    }

    public Nic getAgentNic() {
        return agentNic;
    }

    public void setAgentNic(Nic agentNic) {
        this.agentNic = agentNic;
    }

}