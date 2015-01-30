package io.cattle.platform.core.addon;

import io.github.ibuildthecloud.gdapi.annotation.Type;

@Type(list = false)
public class GlobalLoadBalancerHealthCheck {

    String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    //FIXME - define healthcheck fields based on what Route53 supports

}