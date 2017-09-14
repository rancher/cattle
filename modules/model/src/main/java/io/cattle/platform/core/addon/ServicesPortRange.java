package io.cattle.platform.core.addon;

import io.github.ibuildthecloud.gdapi.annotation.Field;
import io.github.ibuildthecloud.gdapi.annotation.Type;

@Type(list = false)
public class ServicesPortRange {
    Integer startPort;
    Integer endPort;

    @Field(min = 1, max = 65535, required = true)
    public Integer getStartPort() {
        return startPort;
    }

    public void setStartPort(Integer startPort) {
        this.startPort = startPort;
    }

    @Field(min = 1, max = 65535, required = true)
    public Integer getEndPort() {
        return endPort;
    }

    public void setEndPort(Integer endPort) {
        this.endPort = endPort;
    }

    public ServicesPortRange(Integer startPort, Integer endPort) {
        this.startPort = startPort;
        this.endPort = endPort;
    }

    public ServicesPortRange() {
    }
}
