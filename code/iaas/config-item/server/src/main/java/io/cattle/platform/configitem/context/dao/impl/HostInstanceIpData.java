package io.cattle.platform.configitem.context.dao.impl;

import io.cattle.platform.core.model.InstanceHostMap;
import io.cattle.platform.core.model.IpAddress;

public class HostInstanceIpData {
    InstanceHostMap instanceHostMap;
    IpAddress ipAddress;

    public IpAddress getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(IpAddress ipAddress) {
        this.ipAddress = ipAddress;
    }

    public InstanceHostMap getInstanceHostMap() {
        return instanceHostMap;
    }

    public void setInstanceHostMap(InstanceHostMap instanceHostMap) {
        this.instanceHostMap = instanceHostMap;
    }

}
