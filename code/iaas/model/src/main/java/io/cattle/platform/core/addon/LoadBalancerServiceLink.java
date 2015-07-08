package io.cattle.platform.core.addon;

import io.cattle.platform.core.util.LoadBalancerTargetPortSpec;
import io.github.ibuildthecloud.gdapi.annotation.Type;

import java.util.ArrayList;
import java.util.List;

@Type(list = false)
public class LoadBalancerServiceLink extends ServiceLink {
    List<String> ports = new ArrayList<>();;

    public LoadBalancerServiceLink() {
    }

    public LoadBalancerServiceLink(long serviceId, String name, List<String> ports) {
        super(serviceId, name);
        for (String port : ports) {
            // validate the spec
            new LoadBalancerTargetPortSpec(port);
            this.ports.add(port);
        }
    }

    public List<String> getPorts() {
        return ports;
    }

    public void setPorts(List<String> ports) {
        this.ports = ports;
    }
}
