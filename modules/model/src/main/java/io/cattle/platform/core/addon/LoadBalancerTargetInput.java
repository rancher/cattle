package io.cattle.platform.core.addon;

import io.cattle.platform.core.model.Service;

import java.util.ArrayList;
import java.util.List;

public class LoadBalancerTargetInput {
    List<? extends String> ports = new ArrayList<>();
    Service service;

    public void setPorts(List<? extends String> ports) {
        this.ports = ports;
    }

    public List<? extends String> getPorts() {
        return ports;
    }

    public Service getService() {
        return service;
    }

    public void setService(Service service) {
        this.service = service;
    }
}
