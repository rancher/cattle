package io.cattle.platform.configitem.context.data;

public class LoadBalancerTargetInfo {
    private String name;
    private String ipAddress;

    public LoadBalancerTargetInfo(String ipAddress, String name) {
        super();
        this.ipAddress = ipAddress;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

}
