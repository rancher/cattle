package io.cattle.platform.core.addon;

import io.github.ibuildthecloud.gdapi.annotation.Type;

/**
 * HA Proxy format (set on the backend listener)
 * timeout check <responseTimeout>
 * option httpchk <requestLine>
 * server <serverName Ip:port> check inter <interval> rise <healthyThreshold> fall <unhealthyThreshold>
 */

@Type(list = false)
public class LoadBalancerHealthCheck {

    String name;
    Integer responseTimeout;
    Integer interval;
    Integer healthyThreshold;
    Integer unhealthyThreshold;
    String requestLine;

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

    public String getRequestLine() {
        return requestLine;
    }

    public void setRequestLine(String requestLine) {
        this.requestLine = requestLine;
    }
}
