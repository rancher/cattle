package io.cattle.platform.configitem.context.data;

import io.cattle.platform.core.model.IpAddress;
import io.cattle.platform.core.model.Subnet;

public class IpAssociationData {

    IpAddress ipAddress;
    IpAddress targetIpAddress;
    Subnet subnet;

    public IpAssociationData() {
    }

    public IpAssociationData(IpAddress ipAddress, IpAddress targetIpAddress, Subnet subnet) {
        this.ipAddress = ipAddress;
        this.targetIpAddress = targetIpAddress;
        this.subnet = subnet;
    }

    public IpAddress getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(IpAddress ipAddress) {
        this.ipAddress = ipAddress;
    }

    public IpAddress getTargetIpAddress() {
        return targetIpAddress;
    }

    public void setTargetIpAddress(IpAddress targetIpAddress) {
        this.targetIpAddress = targetIpAddress;
    }

    public Subnet getSubnet() {
        return subnet;
    }

    public void setSubnet(Subnet subnet) {
        this.subnet = subnet;
    }


}