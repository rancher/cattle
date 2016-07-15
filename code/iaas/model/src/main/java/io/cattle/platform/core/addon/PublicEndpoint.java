package io.cattle.platform.core.addon;

import io.github.ibuildthecloud.gdapi.annotation.Field;
import io.github.ibuildthecloud.gdapi.annotation.Type;

@Type(list = false)
public class PublicEndpoint {
    String ipAddress;
    Integer port;
    String serviceId;
    String hostId;
    String instanceId;

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
    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    @Field(typeString = "reference[host]")
    public String getHostId() {
        return hostId;
    }

    public void setHostId(String hostId) {
        this.hostId = hostId;
    }

    @Field(typeString = "reference[instance]")
    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public PublicEndpoint(String ipAddress, Integer port, Long hostId, Long instanceId, Long serviceId) {
        this.ipAddress = ipAddress;
        this.port = port;
        if (hostId != null) {
            this.hostId = hostId.toString();
        }
        if (instanceId != null) {
            this.instanceId = instanceId.toString();
        }

        if (serviceId != null) {
            this.serviceId = serviceId.toString();
        }
    }

    public PublicEndpoint() {
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((ipAddress == null) ? 0 : ipAddress.hashCode());
        result = prime * result + ((port == null) ? 0 : port.hashCode());
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
        PublicEndpoint other = (PublicEndpoint) obj;
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
        return true;
    }
}
