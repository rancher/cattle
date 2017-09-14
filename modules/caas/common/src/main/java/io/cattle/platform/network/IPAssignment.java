package io.cattle.platform.network;

import io.cattle.platform.core.model.Subnet;

public class IPAssignment {
    String ipAddress;
    Subnet subnet;

    public IPAssignment(String ipAddress, Subnet subnet) {
        super();
        this.ipAddress = ipAddress;
        this.subnet = subnet;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public Subnet getSubnet() {
        return subnet;
    }
}
