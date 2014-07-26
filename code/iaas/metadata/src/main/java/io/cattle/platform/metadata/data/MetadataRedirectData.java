package io.cattle.platform.metadata.data;

import io.cattle.platform.core.model.IpAddress;
import io.cattle.platform.core.model.Subnet;

public class MetadataRedirectData {

    Subnet subnet;
    IpAddress ipAddress;

    public MetadataRedirectData(Subnet subnet, IpAddress ipAddress) {
        super();
        this.subnet = subnet;
        this.ipAddress = ipAddress;
    }

    public Subnet getSubnet() {
        return subnet;
    }

    public void setSubnet(Subnet subnet) {
        this.subnet = subnet;
    }

    public IpAddress getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(IpAddress ipAddress) {
        this.ipAddress = ipAddress;
    }

}
