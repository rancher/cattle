package io.cattle.platform.configitem.context.data;

public class LoadBalancerTargetInfo {
    private String name;
    private String ipAddress;
    private String cookie;

    public LoadBalancerTargetInfo(String ipAddress, String name, String cookie) {
        super();
        this.ipAddress = ipAddress;
        this.name = name;
        this.cookie = cookie;
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

    public String getCookie() {
        return cookie;
    }

    public void setCookie(String cookie) {
        this.cookie = cookie;
    }
}
