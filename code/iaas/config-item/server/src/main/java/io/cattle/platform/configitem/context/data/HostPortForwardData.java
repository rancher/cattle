package io.cattle.platform.configitem.context.data;

import io.cattle.platform.core.model.IpAddress;
import io.cattle.platform.core.model.Port;

public class HostPortForwardData {

    IpAddress publicIpAddress;
    IpAddress privateIpAddress;
    Port port;

    public HostPortForwardData() {
    }

    public IpAddress getPublicIpAddress() {
        return publicIpAddress;
    }

    public void setPublicIpAddress(IpAddress publicIpAddress) {
        this.publicIpAddress = publicIpAddress;
    }

    public IpAddress getPrivateIpAddress() {
        return privateIpAddress;
    }

    public void setPrivateIpAddress(IpAddress privateIpAddress) {
        this.privateIpAddress = privateIpAddress;
    }

    public Port getPort() {
        return port;
    }

    public void setPort(Port port) {
        this.port = port;
    }

}
