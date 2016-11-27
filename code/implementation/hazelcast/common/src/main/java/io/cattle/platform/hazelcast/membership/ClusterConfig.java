package io.cattle.platform.hazelcast.membership;

public class ClusterConfig {

    String advertiseAddress;
    Integer httpPort;
    boolean clustered;

    public String getAdvertiseAddress() {
        return advertiseAddress;
    }

    public Integer getHttpPort() {
        return httpPort;
    }

    public boolean isClustered() {
        return clustered;
    }

    public void setAdvertiseAddress(String advertiseAddress) {
        this.advertiseAddress = advertiseAddress;
    }

    public void setHttpPort(Integer httpPort) {
        this.httpPort = httpPort;
    }

    public void setClustered(boolean clustered) {
        this.clustered = clustered;
    }

}