package io.cattle.platform.core.addon;

import io.cattle.platform.core.util.PortSpec;
import io.github.ibuildthecloud.gdapi.annotation.Field;
import io.github.ibuildthecloud.gdapi.annotation.Type;
import org.weakref.jmx.com.google.common.base.Objects;

@Type(list = false, name = "publicEndpoint")
public class PortInstance {
    /**
     * The IP the user requested. Typically this is null or 0.0.0.0
     */
    String ipAddress;
    /**
     * The agent IP of the host.  In the situation that bindIpAddress is 0.0.0.0, agentIp helps the UI/CLI show something more useful.
     */
    String agentIpAddress;
    /**
     * The IP the port was scheduled to.  This is in the situation of hosts w/ multiple public IP addresses
     */
    String bindIpAddress;
    String protocol;
    String fqdn;
    Integer publicPort;
    Integer privatePort;
    Long serviceId;
    Long hostId;
    Long instanceId;

    public PortInstance() {
    }

    public boolean isBindAll() {
        return "0.0.0.0".equals(bindIpAddress);
    }

    public PortInstance(Integer privatePort, String procotol) {
        this.privatePort = privatePort;
        this.protocol = procotol;
    }

    public PortInstance(PortSpec spec) {
        this.ipAddress = spec.getIpAddress();
        this.bindIpAddress = spec.getIpAddress();
        this.publicPort = spec.getPublicPort();
        this.privatePort = spec.getPrivatePort();
        this.protocol = spec.getProtocol();
    }

    public boolean matches(PortSpec spec) {
        if (spec == null) {
            return false;
        }
        return Objects.equal(getIpAddress(), spec.getIpAddress()) &&
                Objects.equal(getPublicPort(), spec.getPublicPort()) &&
                Objects.equal(getProtocol(), spec.getProtocol());
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public Integer getPublicPort() {
        return publicPort;
    }

    public void setPublicPort(Integer port) {
        this.publicPort = port;
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

    public String getProtocol() {
        return protocol == null ? "tcp" : protocol;
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

    public String getBindIpAddress() {
        if (bindIpAddress == null) {
            return "0.0.0.0";
        }
        return bindIpAddress;
    }

    public void setBindIpAddress(String bindIpAddress) {
        this.bindIpAddress = bindIpAddress;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((agentIpAddress == null) ? 0 : agentIpAddress.hashCode());
        result = prime * result + ((bindIpAddress == null) ? 0 : bindIpAddress.hashCode());
        result = prime * result + ((fqdn == null) ? 0 : fqdn.hashCode());
        result = prime * result + ((hostId == null) ? 0 : hostId.hashCode());
        result = prime * result + ((instanceId == null) ? 0 : instanceId.hashCode());
        result = prime * result + ((ipAddress == null) ? 0 : ipAddress.hashCode());
        result = prime * result + ((privatePort == null) ? 0 : privatePort.hashCode());
        result = prime * result + ((protocol == null) ? 0 : protocol.hashCode());
        result = prime * result + ((publicPort == null) ? 0 : publicPort.hashCode());
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
        PortInstance other = (PortInstance) obj;
        if (agentIpAddress == null) {
            if (other.agentIpAddress != null)
                return false;
        } else if (!agentIpAddress.equals(other.agentIpAddress))
            return false;
        if (bindIpAddress == null) {
            if (other.bindIpAddress != null)
                return false;
        } else if (!bindIpAddress.equals(other.bindIpAddress))
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
        if (publicPort == null) {
            if (other.publicPort != null)
                return false;
        } else if (!publicPort.equals(other.publicPort))
            return false;
        if (serviceId == null) {
            if (other.serviceId != null)
                return false;
        } else if (!serviceId.equals(other.serviceId))
            return false;
        return true;
    }

}
