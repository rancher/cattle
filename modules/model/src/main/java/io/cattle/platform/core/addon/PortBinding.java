package io.cattle.platform.core.addon;

import io.github.ibuildthecloud.gdapi.annotation.Field;
import io.github.ibuildthecloud.gdapi.annotation.Type;

@Type(list = false)
public class PortBinding {
    String ipAddress;
    String agentIpAddress;
    String bindAddress;
    String protocol;
    String fqdn;
    Integer port;
    Integer privatePort;
    Long serviceId;
    Long hostId;
    Long instanceId;

    public PortBinding() {
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    @Field(typeString = "reference[service]")
    public Long getServiceId() {
        return serviceId;
    }

    public void setServiceId(Long serviceId) {
        this.serviceId = serviceId;
    }

    @Field(typeString = "reference[host]")
    public Long getHostId() {
        return hostId;
    }

    public void setHostId(Long hostId) {
        this.hostId = hostId;
    }

    @Field(typeString = "reference[instance]")
    public Long getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(Long instanceId) {
        this.instanceId = instanceId;
    }

    public Integer getPrivatePort() {
        return privatePort;
    }

    public void setPrivatePort(Integer privatePort) {
        this.privatePort = privatePort;
    }

    public String getBindAddress() {
        return bindAddress;
    }

    public void setBindAddress(String bindAddress) {
        this.bindAddress = bindAddress;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getAgentIpAddress() {
        return agentIpAddress;
    }

    public void setAgentIpAddress(String agentIpAddress) {
        this.agentIpAddress = agentIpAddress;
    }

    public String getFqdn() {
        return fqdn;
    }

    public void setFqdn(String fqdn) {
        this.fqdn = fqdn;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((agentIpAddress == null) ? 0 : agentIpAddress.hashCode());
        result = prime * result + ((bindAddress == null) ? 0 : bindAddress.hashCode());
        result = prime * result + ((fqdn == null) ? 0 : fqdn.hashCode());
        result = prime * result + ((hostId == null) ? 0 : hostId.hashCode());
        result = prime * result + ((instanceId == null) ? 0 : instanceId.hashCode());
        result = prime * result + ((ipAddress == null) ? 0 : ipAddress.hashCode());
        result = prime * result + ((port == null) ? 0 : port.hashCode());
        result = prime * result + ((privatePort == null) ? 0 : privatePort.hashCode());
        result = prime * result + ((protocol == null) ? 0 : protocol.hashCode());
        result = prime * result + ((serviceId == null) ? 0 : serviceId.hashCode());
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
        PortBinding other = (PortBinding) obj;
        if (agentIpAddress == null) {
            if (other.agentIpAddress != null)
                return false;
        } else if (!agentIpAddress.equals(other.agentIpAddress))
            return false;
        if (bindAddress == null) {
            if (other.bindAddress != null)
                return false;
        } else if (!bindAddress.equals(other.bindAddress))
            return false;
        if (fqdn == null) {
            if (other.fqdn != null)
                return false;
        } else if (!fqdn.equals(other.fqdn))
            return false;
        if (hostId == null) {
            if (other.hostId != null)
                return false;
        } else if (!hostId.equals(other.hostId))
            return false;
        if (instanceId == null) {
            if (other.instanceId != null)
                return false;
        } else if (!instanceId.equals(other.instanceId))
            return false;
        if (ipAddress == null) {
            if (other.ipAddress != null)
                return false;
        } else if (!ipAddress.equals(other.ipAddress))
            return false;
        if (port == null) {
            if (other.port != null)
                return false;
        } else if (!port.equals(other.port))
            return false;
        if (privatePort == null) {
            if (other.privatePort != null)
                return false;
        } else if (!privatePort.equals(other.privatePort))
            return false;
        if (protocol == null) {
            if (other.protocol != null)
                return false;
        } else if (!protocol.equals(other.protocol))
            return false;
        if (serviceId == null) {
            if (other.serviceId != null)
                return false;
        } else if (!serviceId.equals(other.serviceId))
            return false;
        return true;
    }

}
