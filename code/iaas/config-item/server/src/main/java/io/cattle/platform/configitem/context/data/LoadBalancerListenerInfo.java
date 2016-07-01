package io.cattle.platform.configitem.context.data;

import io.cattle.platform.core.model.Service;

public class LoadBalancerListenerInfo {
    Integer privatePort;
    Integer sourcePort;
    Integer targetPort;
    String sourceProtocol;
    String targetProtocol;
    String uuid;
    Service lbSvc;
    boolean proxyPort;

    public LoadBalancerListenerInfo(Service lbSvc, Integer privatePort, Integer sourcePort, String protocol,
            Integer targetPort, boolean proxyPort) {
        super();
        this.privatePort = privatePort;
        this.sourcePort = sourcePort;
        this.sourceProtocol = protocol;
        this.targetProtocol = protocol;
        this.targetPort = targetPort;
        this.lbSvc = lbSvc;
        setUuid();
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

    public String getTargetProtocol() {
        return targetProtocol;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid() {
        Integer listenerPort = privatePort != null ? privatePort : sourcePort;
        this.uuid = lbSvc.getUuid() + "_" + listenerPort.toString();
    }

    public boolean isProxyPort() {
        return proxyPort;
    }

}
