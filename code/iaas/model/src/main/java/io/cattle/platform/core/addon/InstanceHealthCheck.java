package io.cattle.platform.core.addon;

import io.github.ibuildthecloud.gdapi.annotation.Field;
import io.github.ibuildthecloud.gdapi.annotation.Type;

@Type(list = false)
public class InstanceHealthCheck {
    String name;
    Integer responseTimeout;
    Integer interval;
    Integer healthyThreshold;
    Integer unhealthyThreshold;
    String requestLine;
    Integer port;

    @Field(nullable = true)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getResponseTimeout() {
        return responseTimeout;
    }

    public void setResponseTimeout(Integer responseTimeout) {
        this.responseTimeout = responseTimeout;
    }

    public Integer getInterval() {
        return interval;
    }

    public void setInterval(Integer interval) {
        this.interval = interval;
    }

    public Integer getHealthyThreshold() {
        return healthyThreshold;
    }

    public void setHealthyThreshold(Integer healthyThreshold) {
        this.healthyThreshold = healthyThreshold;
    }

    public Integer getUnhealthyThreshold() {
        return unhealthyThreshold;
    }

    public void setUnhealthyThreshold(Integer unhealthyThreshold) {
        this.unhealthyThreshold = unhealthyThreshold;
    }

    @Field(nullable = true)
    public String getRequestLine() {
        return requestLine;
    }

    public void setRequestLine(String requestLine) {
        this.requestLine = requestLine;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }
}
