package io.cattle.platform.configitem.context.dao.impl;

import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.IpAddress;

public class HostInstanceIpData {
    Instance instance;
    IpAddress ipAddress;

    public Instance getInstance() {
        return instance;
    }

    public IpAddress getIpAddress() {
        return ipAddress;
    }

    public void setInstance(Instance instance) {
        this.instance = instance;
    }

    public void setIpAddress(IpAddress ipAddress) {
        this.ipAddress = ipAddress;
    }

}
