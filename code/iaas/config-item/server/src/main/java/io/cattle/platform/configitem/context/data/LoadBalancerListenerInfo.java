package io.cattle.platform.configitem.context.data;


public class LoadBalancerListenerInfo {
    Integer privatePort;
    Integer sourcePort;
    Integer targetPort;
    String sourceProtocol;
    boolean proxyPort;

    public LoadBalancerListenerInfo(Integer privatePort, Integer sourcePort, String protocol, Integer targetPort,
            boolean proxyPort) {
        super();
        this.privatePort = privatePort;
        this.sourcePort = sourcePort;
        this.sourceProtocol = protocol;
        this.targetPort = targetPort;
        this.proxyPort = proxyPort;
    }

    // LEGACY code to support the case when private port is not defined
    public Integer getSourcePort() {
        return this.privatePort != null ? this.privatePort : this.sourcePort;
    }

    public Integer getTargetPort() {
        return targetPort;
    }

    public String getSourceProtocol() {
        return sourceProtocol;
    }

    public boolean isProxyPort() {
        return proxyPort;
    }

}
