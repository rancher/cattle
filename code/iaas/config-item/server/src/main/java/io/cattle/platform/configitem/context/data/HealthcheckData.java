package io.cattle.platform.configitem.context.data;

import io.cattle.platform.core.model.IpAddress;
import io.cattle.platform.core.model.Service;

public class HealthcheckData {
    IpAddress targetIpAddress;
    Service service;

    public HealthcheckData() {
    }

    public IpAddress getTargetIpAddress() {
        return targetIpAddress;
    }

    public void setTargetIpAddress(IpAddress targetIpAddress) {
        this.targetIpAddress = targetIpAddress;
    }

    public Service getService() {
        return service;
    }

    public void setService(Service service) {
        this.service = service;
    }
}
