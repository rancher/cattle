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
    String service;
    Integer targetPort;
    String backendName;
    String selector;
    String instance;

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

    public PortRule(PortRule other) {
        this.hostname = other.hostname;
        this.path = other.path;
        this.sourcePort = other.sourcePort;
        this.priority = other.priority;
        this.protocol = other.protocol;
        this.service = other.service;
        this.targetPort = other.targetPort;
        this.backendName = other.backendName;
        this.selector = other.selector;
        this.instance = other.instance;
    }

    public PortRule(String hostname, String path, Integer sourcePort, Integer priority, Protocol protocol,
            String service, Integer targetPort, String backendName, String selector, String instance) {
        super();
        this.hostname = hostname;
        this.path = path;
        this.sourcePort = sourcePort;
        this.priority = priority;
        this.protocol = protocol;
        this.service = service;
        this.targetPort = targetPort;
        this.backendName = backendName;
        this.selector = selector;
        this.instance = instance;
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

    @Field(min = 1, max = 65535)
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

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
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

    public String getInstance() {
        return instance;
    }

    public void setInstance(String instance) {
        this.instance = instance;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((backendName == null) ? 0 : backendName.hashCode());
        result = prime * result + ((hostname == null) ? 0 : hostname.hashCode());
        result = prime * result + ((instance == null) ? 0 : instance.hashCode());
        result = prime * result + ((path == null) ? 0 : path.hashCode());
        result = prime * result + ((priority == null) ? 0 : priority.hashCode());
        result = prime * result + ((protocol == null) ? 0 : protocol.hashCode());
        result = prime * result + ((selector == null) ? 0 : selector.hashCode());
        result = prime * result + ((service == null) ? 0 : service.hashCode());
        result = prime * result + ((sourcePort == null) ? 0 : sourcePort.hashCode());
        result = prime * result + ((targetPort == null) ? 0 : targetPort.hashCode());
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
        PortRule other = (PortRule) obj;
        if (backendName == null) {
            if (other.backendName != null)
                return false;
        } else if (!backendName.equals(other.backendName))
            return false;
        if (hostname == null) {
            if (other.hostname != null)
                return false;
        } else if (!hostname.equals(other.hostname))
            return false;
        if (instance == null) {
            if (other.instance != null)
                return false;
        } else if (!instance.equals(other.instance))
            return false;
        if (path == null) {
            if (other.path != null)
                return false;
        } else if (!path.equals(other.path))
            return false;
        if (priority == null) {
            if (other.priority != null)
                return false;
        } else if (!priority.equals(other.priority))
            return false;
        if (protocol != other.protocol)
            return false;
        if (selector == null) {
            if (other.selector != null)
                return false;
        } else if (!selector.equals(other.selector))
            return false;
        if (service == null) {
            if (other.service != null)
                return false;
        } else if (!service.equals(other.service))
            return false;
        if (sourcePort == null) {
            if (other.sourcePort != null)
                return false;
        } else if (!sourcePort.equals(other.sourcePort))
            return false;
        if (targetPort == null) {
            if (other.targetPort != null)
                return false;
        } else if (!targetPort.equals(other.targetPort))
            return false;
        return true;
    }

}
