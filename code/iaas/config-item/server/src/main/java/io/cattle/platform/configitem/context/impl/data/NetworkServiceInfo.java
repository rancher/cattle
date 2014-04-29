package io.cattle.platform.configitem.context.impl.data;

import io.cattle.platform.core.model.Network;
import io.cattle.platform.core.model.NetworkService;
import io.cattle.platform.core.model.Nic;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class NetworkServiceInfo {

    String kind;
    NetworkService service;
    List<Nic> nics = new ArrayList<Nic>();
    Set<Long> nicIds = new HashSet<Long>();
    List<Network> networks = new ArrayList<Network>();
    Set<Long> networkIds = new HashSet<Long>();

    public NetworkServiceInfo(NetworkService networkService) {
        super();
        this.service = networkService;
        this.kind = networkService.getKind();
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public List<Nic> getNics() {
        return nics;
    }

    public void setNics(List<Nic> nics) {
        this.nics = nics;
    }

    public Set<Long> getNicIds() {
        return nicIds;
    }

    public void setNicIds(Set<Long> nicIds) {
        this.nicIds = nicIds;
    }

    public List<Network> getNetworks() {
        return networks;
    }

    public void setNetworks(List<Network> networks) {
        this.networks = networks;
    }

    public Set<Long> getNetworkIds() {
        return networkIds;
    }

    public void setNetworkIds(Set<Long> networkIds) {
        this.networkIds = networkIds;
    }

    public NetworkService getService() {
        return service;
    }

    public void setService(NetworkService service) {
        this.service = service;
    }

}