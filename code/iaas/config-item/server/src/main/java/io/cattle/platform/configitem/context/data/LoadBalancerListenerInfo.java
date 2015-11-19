package io.cattle.platform.configitem.context.data;

import java.util.UUID;

public class LoadBalancerListenerInfo {
    Integer privatePort;
    Integer sourcePort;
    Integer targetPort;
    String sourceProtocol;
    String targetProtocol;
    String uuid;
    String algorithm = "roundrobin";

    public LoadBalancerListenerInfo(Integer privatePort, Integer sourcePort, String protocol, Integer targetPort) {
        super();
        this.privatePort = privatePort;
        this.sourcePort = sourcePort;
        this.sourceProtocol = protocol;
        this.targetProtocol = protocol;
        this.targetPort = targetPort;
        this.uuid = UUID.randomUUID().toString();
    }

    public Integer getPrivatePort() {
        return privatePort;
    }

    public Integer getSourcePort() {
        return sourcePort;
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

    public String getAlgorithm() {
        return algorithm;
    }
}
