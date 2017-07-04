package io.cattle.platform.metadata.model;

import io.cattle.platform.core.addon.HealthcheckState;
import io.cattle.platform.core.addon.InstanceHealthCheck;
import io.cattle.platform.core.addon.PortInstance;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.object.util.DataAccessor;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class InstanceInfo implements MetadataObject {
    long id;
    long networkFromContainerId;

    String name;
    String hostname;
    String uuid;
    String healthState;
    String state;
    String externalId;
    String primaryIp;
    String primaryMacAddress;
    Integer serviceIndex;

    boolean system;

    Long agentId;
    Long serviceId;
    Long stackId;
    Long hostId;
    Long memoryReservation;
    Long milliCpuReservation;
    Long networkId;
    Long startCount;

    Set<Long> serviceIds;
    Set<PortInstance> ports;
    List<String> dns;
    List<String> dnsSearch;
    Map<String, String> labels;
    Map<String, Object> links;
    List<HealthcheckState> healthCheckHosts;
    HealthcheckInfo healthCheck;

    public InstanceInfo(Instance instance) {
        this.id = instance.getId();
        this.hostname = instance.getHostname();
        this.name = instance.getName();
        this.uuid = instance.getUuid();
        this.healthState = instance.getHealthState();
        this.state = instance.getState();
        this.externalId = instance.getExternalId();
        this.primaryIp = DataAccessor.fieldString(instance, InstanceConstants.FIELD_PRIMARY_IP_ADDRESS);
        this.primaryMacAddress = DataAccessor.fieldString(instance, InstanceConstants.FIELD_PRIMARY_MAC_ADDRESSS);
        this.serviceIndex = instance.getServiceIndex();
        this.networkFromContainerId = instance.getNetworkContainerId();
        this.system = instance.getSystem();
        this.serviceId = instance.getServiceId();
        this.stackId = instance.getStackId();
        this.hostId = instance.getHostId();
        this.memoryReservation = instance.getMemoryReservation();
        this.milliCpuReservation = instance.getMilliCpuReservation();
        this.networkId = instance.getNetworkId();
        this.startCount = instance.getStartCount();
        this.ports = new HashSet<>(DataAccessor.fieldObjectList(instance, InstanceConstants.FIELD_PORT_BINDINGS, PortInstance.class));
        this.dns = DataAccessor.fieldStringList(instance, InstanceConstants.FIELD_DNS);
        this.dnsSearch = DataAccessor.fieldStringList(instance, InstanceConstants.FIELD_DNS_SEARCH);
        this.labels = DataAccessor.getLabels(instance);
        this.healthCheckHosts = DataAccessor.fieldObjectList(instance,
                InstanceConstants.FIELD_HEALTHCHECK_STATES, HealthcheckState.class);
        this.links = DataAccessor.fieldMapRO(instance, InstanceConstants.FIELD_INSTANCE_LINKS);
        this.agentId = instance.getAccountId();
        this.serviceIds = new HashSet<>(DataAccessor.fieldLongList(instance, InstanceConstants.FIELD_SERVICE_IDS));

        InstanceHealthCheck hc = DataAccessor.field(instance, InstanceConstants.FIELD_HEALTH_CHECK, InstanceHealthCheck.class);
        if (hc != null) {
            this.healthCheck = new HealthcheckInfo(hc);
        }
    }

    public long getId() {
        return id;
    }

    public String getHostname() {
        return hostname;
    }

    public String getName() {
        return name;
    }

    @Override
    public String getUuid() {
        return uuid;
    }

    public String getHealthState() {
        return healthState;
    }

    public String getState() {
        return state;
    }

    public String getExternalId() {
        return externalId;
    }

    public String getPrimaryIp() {
        return primaryIp;
    }

    public String getPrimaryMacAddress() {
        return primaryMacAddress;
    }

    public Integer getServiceIndex() {
        return serviceIndex;
    }

    public long getNetworkFromContainerId() {
        return networkFromContainerId;
    }

    public boolean isSystem() {
        return system;
    }

    public Long getServiceId() {
        return serviceId;
    }

    public Long getStackId() {
        return stackId;
    }

    public Long getHostId() {
        return hostId;
    }

    public Long getMemoryReservation() {
        return memoryReservation;
    }

    public Long getMilliCpuReservation() {
        return milliCpuReservation;
    }

    public Long getNetworkId() {
        return networkId;
    }

    public Long getStartCount() {
        return startCount;
    }

    public List<String> getDns() {
        return dns;
    }

    public List<String> getDnsSearch() {
        return dnsSearch;
    }

    public Map<String, String> getLabels() {
        return labels;
    }

    public List<HealthcheckState> getHealthCheckHosts() {
        return healthCheckHosts;
    }

    public Map<String, Object> getLinks() {
        return links;
    }

    public Long getAgentId() {
        return agentId;
    }

    public HealthcheckInfo getHealthCheck() {
        return healthCheck;
    }

    public Set<PortInstance> getPorts() {
        return ports;
    }

    public Set<Long> getServiceIds() {
        return serviceIds;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((agentId == null) ? 0 : agentId.hashCode());
        result = prime * result + ((dns == null) ? 0 : dns.hashCode());
        result = prime * result + ((dnsSearch == null) ? 0 : dnsSearch.hashCode());
        result = prime * result + ((externalId == null) ? 0 : externalId.hashCode());
        result = prime * result + ((healthCheck == null) ? 0 : healthCheck.hashCode());
        result = prime * result + ((healthCheckHosts == null) ? 0 : healthCheckHosts.hashCode());
        result = prime * result + ((healthState == null) ? 0 : healthState.hashCode());
        result = prime * result + ((hostId == null) ? 0 : hostId.hashCode());
        result = prime * result + ((hostname == null) ? 0 : hostname.hashCode());
        result = prime * result + (int) (id ^ (id >>> 32));
        result = prime * result + ((labels == null) ? 0 : labels.hashCode());
        result = prime * result + ((links == null) ? 0 : links.hashCode());
        result = prime * result + ((memoryReservation == null) ? 0 : memoryReservation.hashCode());
        result = prime * result + ((milliCpuReservation == null) ? 0 : milliCpuReservation.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + (int) (networkFromContainerId ^ (networkFromContainerId >>> 32));
        result = prime * result + ((networkId == null) ? 0 : networkId.hashCode());
        result = prime * result + ((ports == null) ? 0 : ports.hashCode());
        result = prime * result + ((primaryIp == null) ? 0 : primaryIp.hashCode());
        result = prime * result + ((primaryMacAddress == null) ? 0 : primaryMacAddress.hashCode());
        result = prime * result + ((serviceId == null) ? 0 : serviceId.hashCode());
        result = prime * result + ((serviceIds == null) ? 0 : serviceIds.hashCode());
        result = prime * result + ((serviceIndex == null) ? 0 : serviceIndex.hashCode());
        result = prime * result + ((stackId == null) ? 0 : stackId.hashCode());
        result = prime * result + ((startCount == null) ? 0 : startCount.hashCode());
        result = prime * result + ((state == null) ? 0 : state.hashCode());
        result = prime * result + (system ? 1231 : 1237);
        result = prime * result + ((uuid == null) ? 0 : uuid.hashCode());
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
        InstanceInfo other = (InstanceInfo) obj;
        if (agentId == null) {
            if (other.agentId != null)
                return false;
        } else if (!agentId.equals(other.agentId))
            return false;
        if (dns == null) {
            if (other.dns != null)
                return false;
        } else if (!dns.equals(other.dns))
            return false;
        if (dnsSearch == null) {
            if (other.dnsSearch != null)
                return false;
        } else if (!dnsSearch.equals(other.dnsSearch))
            return false;
        if (externalId == null) {
            if (other.externalId != null)
                return false;
        } else if (!externalId.equals(other.externalId))
            return false;
        if (healthCheck == null) {
            if (other.healthCheck != null)
                return false;
        } else if (!healthCheck.equals(other.healthCheck))
            return false;
        if (healthCheckHosts == null) {
            if (other.healthCheckHosts != null)
                return false;
        } else if (!healthCheckHosts.equals(other.healthCheckHosts))
            return false;
        if (healthState == null) {
            if (other.healthState != null)
                return false;
        } else if (!healthState.equals(other.healthState))
            return false;
        if (hostId == null) {
            if (other.hostId != null)
                return false;
        } else if (!hostId.equals(other.hostId))
            return false;
        if (hostname == null) {
            if (other.hostname != null)
                return false;
        } else if (!hostname.equals(other.hostname))
            return false;
        if (id != other.id)
            return false;
        if (labels == null) {
            if (other.labels != null)
                return false;
        } else if (!labels.equals(other.labels))
            return false;
        if (links == null) {
            if (other.links != null)
                return false;
        } else if (!links.equals(other.links))
            return false;
        if (memoryReservation == null) {
            if (other.memoryReservation != null)
                return false;
        } else if (!memoryReservation.equals(other.memoryReservation))
            return false;
        if (milliCpuReservation == null) {
            if (other.milliCpuReservation != null)
                return false;
        } else if (!milliCpuReservation.equals(other.milliCpuReservation))
            return false;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        if (networkFromContainerId != other.networkFromContainerId)
            return false;
        if (networkId == null) {
            if (other.networkId != null)
                return false;
        } else if (!networkId.equals(other.networkId))
            return false;
        if (ports == null) {
            if (other.ports != null)
                return false;
        } else if (!ports.equals(other.ports))
            return false;
        if (primaryIp == null) {
            if (other.primaryIp != null)
                return false;
        } else if (!primaryIp.equals(other.primaryIp))
            return false;
        if (primaryMacAddress == null) {
            if (other.primaryMacAddress != null)
                return false;
        } else if (!primaryMacAddress.equals(other.primaryMacAddress))
            return false;
        if (serviceId == null) {
            if (other.serviceId != null)
                return false;
        } else if (!serviceId.equals(other.serviceId))
            return false;
        if (serviceIds == null) {
            if (other.serviceIds != null)
                return false;
        } else if (!serviceIds.equals(other.serviceIds))
            return false;
        if (serviceIndex == null) {
            if (other.serviceIndex != null)
                return false;
        } else if (!serviceIndex.equals(other.serviceIndex))
            return false;
        if (stackId == null) {
            if (other.stackId != null)
                return false;
        } else if (!stackId.equals(other.stackId))
            return false;
        if (startCount == null) {
            if (other.startCount != null)
                return false;
        } else if (!startCount.equals(other.startCount))
            return false;
        if (state == null) {
            if (other.state != null)
                return false;
        } else if (!state.equals(other.state))
            return false;
        if (system != other.system)
            return false;
        if (uuid == null) {
            if (other.uuid != null)
                return false;
        } else if (!uuid.equals(other.uuid))
            return false;
        return true;
    }

}

