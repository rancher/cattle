package io.cattle.platform.core.addon;

import io.github.ibuildthecloud.gdapi.annotation.Field;
import io.github.ibuildthecloud.gdapi.annotation.Type;

@Type(list = false)
public class PortRule {

    String hostname;
    String path;
    Integer sourcePort;
    Integer priority;
    Protocol protocol;
    String serviceId;
    Integer targetPort;
    String backendName;
    String selector;
    
    public enum Protocol {
        http,
        tcp,
        https,
        tls,
        sni,
        udp
    }

    public PortRule() {
    }

    public PortRule(String hostname, String path, Integer sourcePort, Integer priority, Protocol protocol,
            String serviceId,
            Integer targetPort, String backendName, String selector) {
        super();
        this.hostname = hostname;
        this.path = path;
        this.sourcePort = sourcePort;
        this.priority = priority;
        this.protocol = protocol;
        this.serviceId = serviceId;
        this.targetPort = targetPort;
        this.backendName = backendName;
        this.selector = selector;
    }

    @Field(nullable = true)
    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    @Field(nullable = true)
    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    @Field(min = 1, max = 65535, required = true)
    public Integer getSourcePort() {
        return sourcePort;
    }

    public void setSourcePort(Integer port) {
        this.sourcePort = port;
    }

    @Field(min = 1, nullable = true)
    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    @Field(defaultValue = "http")
    public Protocol getProtocol() {
        return protocol;
    }

    public void setProtocol(Protocol protocol) {
        this.protocol = protocol;
    }

    @Field(typeString = "reference[service]", nullable = true)
    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    @Field(min = 1, max = 65535, nullable = true)
    public Integer getTargetPort() {
        return targetPort;
    }

    public void setTargetPort(Integer targetPort) {
        this.targetPort = targetPort;
    }

    @Field(nullable = true)
    public String getBackendName() {
        return backendName;
    }

    public void setBackendName(String backendName) {
        this.backendName = backendName;
    }

    @Field(nullable = true)
    public String getSelector() {
        return selector;
    }

    public void setSelector(String selector) {
        this.selector = selector;
    }

}
