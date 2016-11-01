package io.cattle.platform.metadata.data;

import io.cattle.platform.core.model.Credential;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.IpAddress;
import io.cattle.platform.core.model.Network;
import io.cattle.platform.core.model.Nic;
import io.cattle.platform.core.model.Subnet;
import io.cattle.platform.core.model.Volume;

public class MetadataEntry {

    Instance instance;
    Nic nic;
    IpAddress localIp;
    Volume volume;
    Credential credential;
    Network network;
    Subnet subnet;

    public MetadataEntry(Instance instance, Nic nic, IpAddress localIp, Volume volume, Credential credential,
            Network network, Subnet subnet) {
        super();
        this.instance = instance;
        this.nic = nic;
        this.localIp = localIp;
        this.volume = volume;
        this.credential = credential;
        this.network = network;
        this.subnet = subnet;
    }

    public Instance getInstance() {
        return instance;
    }

    public void setInstance(Instance instance) {
        this.instance = instance;
    }

    public Nic getNic() {
        return nic;
    }

    public void setNic(Nic nic) {
        this.nic = nic;
    }

    public IpAddress getLocalIp() {
        return localIp;
    }

    public void setLocalIp(IpAddress localIp) {
        this.localIp = localIp;
    }

    public Volume getVolume() {
        return volume;
    }

    public void setVolume(Volume volume) {
        this.volume = volume;
    }

    public Credential getCredential() {
        return credential;
    }

    public void setCredential(Credential credential) {
        this.credential = credential;
    }

    public Network getNetwork() {
        return network;
    }

    public void setNetwork(Network network) {
        this.network = network;
    }

    public Subnet getSubnet() {
        return subnet;
    }

    public void setSubnet(Subnet subnet) {
        this.subnet = subnet;
    }

}