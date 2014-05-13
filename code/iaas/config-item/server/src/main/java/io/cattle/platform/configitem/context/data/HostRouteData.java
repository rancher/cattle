package io.cattle.platform.configitem.context.data;

import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Subnet;

public class HostRouteData {

    Instance instance;
    Subnet subnet;

    public Subnet getSubnet() {
        return subnet;
    }

    public void setSubnet(Subnet subnet) {
        this.subnet = subnet;
    }

    public Instance getInstance() {
        return instance;
    }

    public void setInstance(Instance instance) {
        this.instance = instance;
    }

}
