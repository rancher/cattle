package io.cattle.platform.core.addon;

import io.cattle.platform.core.util.PortSpec;
import io.github.ibuildthecloud.gdapi.annotation.Field;
import io.github.ibuildthecloud.gdapi.annotation.Type;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
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
        return "0.0.0.0".equals(getBindIpAddress());
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
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        PortInstance that = (PortInstance) o;

        return new EqualsBuilder()
                .append(ipAddress, that.ipAddress)
                .append(agentIpAddress, that.agentIpAddress)
                .append(bindIpAddress, that.bindIpAddress)
                .append(protocol, that.protocol)
                .append(fqdn, that.fqdn)
                .append(publicPort, that.publicPort)
                .append(privatePort, that.privatePort)
                .append(serviceId, that.serviceId)
                .append(hostId, that.hostId)
                .append(instanceId, that.instanceId)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(ipAddress)
                .append(agentIpAddress)
                .append(bindIpAddress)
                .append(protocol)
                .append(fqdn)
                .append(publicPort)
                .append(privatePort)
                .append(serviceId)
                .append(hostId)
                .append(instanceId)
                .toHashCode();
    }
}
