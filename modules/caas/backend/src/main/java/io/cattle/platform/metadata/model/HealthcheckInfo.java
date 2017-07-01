package io.cattle.platform.metadata.model;

import io.cattle.platform.core.addon.InstanceHealthCheck;

public class HealthcheckInfo {
    Integer responseTimeout;
    Integer interval;
    Integer healthyThreshold;
    Integer unhealthyThreshold;
    Integer initializingTimeout;
    String requestLine;
    Integer port;

    public HealthcheckInfo(InstanceHealthCheck hc) {
        this.responseTimeout = hc.getResponseTimeout();
        this.interval = hc.getInterval();
        this.healthyThreshold = hc.getHealthyThreshold();
        this.unhealthyThreshold = hc.getUnhealthyThreshold();
        this.requestLine = hc.getRequestLine();
        this.port = hc.getPort();
        this.initializingTimeout = hc.getInitializingTimeout();
    }

    public Integer getResponseTimeout() {
        return responseTimeout;
    }

    public Integer getInterval() {
        return interval;
    }

    public Integer getHealthyThreshold() {
        return healthyThreshold;
    }

    public Integer getUnhealthyThreshold() {
        return unhealthyThreshold;
    }

    public String getRequestLine() {
        return requestLine;
    }

    public Integer getPort() {
        return port;
    }

    public Integer getInitializingTimeout() {
        return initializingTimeout;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((healthyThreshold == null) ? 0 : healthyThreshold.hashCode());
        result = prime * result + ((initializingTimeout == null) ? 0 : initializingTimeout.hashCode());
        result = prime * result + ((interval == null) ? 0 : interval.hashCode());
        result = prime * result + ((port == null) ? 0 : port.hashCode());
        result = prime * result + ((requestLine == null) ? 0 : requestLine.hashCode());
        result = prime * result + ((responseTimeout == null) ? 0 : responseTimeout.hashCode());
        result = prime * result + ((unhealthyThreshold == null) ? 0 : unhealthyThreshold.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        HealthcheckInfo other = (HealthcheckInfo) obj;
        if (healthyThreshold == null) {
            if (other.healthyThreshold != null)
                return false;
        } else if (!healthyThreshold.equals(other.healthyThreshold))
            return false;
        if (initializingTimeout == null) {
            if (other.initializingTimeout != null)
                return false;
        } else if (!initializingTimeout.equals(other.initializingTimeout))
            return false;
        if (interval == null) {
            if (other.interval != null)
                return false;
        } else if (!interval.equals(other.interval))
            return false;
        if (port == null) {
            if (other.port != null)
                return false;
        } else if (!port.equals(other.port))
            return false;
        if (requestLine == null) {
            if (other.requestLine != null)
                return false;
        } else if (!requestLine.equals(other.requestLine))
            return false;
        if (responseTimeout == null) {
            if (other.responseTimeout != null)
                return false;
        } else if (!responseTimeout.equals(other.responseTimeout))
            return false;
        if (unhealthyThreshold == null) {
            if (other.unhealthyThreshold != null)
                return false;
        } else if (!unhealthyThreshold.equals(other.unhealthyThreshold))
            return false;
        return true;
    }

}
