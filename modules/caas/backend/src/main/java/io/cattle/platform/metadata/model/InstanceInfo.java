package io.cattle.platform.metadata.model;

import io.cattle.platform.core.addon.HealthcheckState;
import io.cattle.platform.core.addon.InstanceHealthCheck;
import io.cattle.platform.core.addon.Link;
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
    Long networkFromContainerId;

    String name;
    String hostname;
    String uuid;
    String healthState;
    String state;
    String externalId;
    String primaryIp;
    String primaryMacAddress;
    Integer serviceIndex;
    Integer exitCode;

    boolean system;
    boolean shouldRestart;

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
    List<Link> links;
    List<HealthcheckState> healthCheckHosts;
    HealthcheckInfo healthCheck;

    public InstanceInfo(Instance instance) {
        this.agentId = instance.getAgentId();
        this.dns = DataAccessor.fieldStringList(instance, InstanceConstants.FIELD_DNS);
        this.dnsSearch = DataAccessor.fieldStringList(instance, InstanceConstants.FIELD_DNS_SEARCH);
        this.externalId = instance.getExternalId();
        this.exitCode = DataAccessor.fieldInteger(instance, InstanceConstants.FIELD_EXIT_CODE);
        this.healthCheckHosts = DataAccessor.fieldObjectList(instance, InstanceConstants.FIELD_HEALTHCHECK_STATES, HealthcheckState.class);
        this.healthState = instance.getHealthState();
        this.hostId = instance.getHostId();
        this.hostname = instance.getHostname();
        this.id = instance.getId();
        this.labels = DataAccessor.getLabels(instance);
        this.links = DataAccessor.fieldObjectList(instance, InstanceConstants.FIELD_LINKS, Link.class);
        this.memoryReservation = instance.getMemoryReservation();
        this.milliCpuReservation = instance.getMilliCpuReservation();
        this.name = instance.getName();
        this.networkFromContainerId = instance.getNetworkContainerId();
        this.networkId = instance.getNetworkId();
        this.ports = new HashSet<>(DataAccessor.fieldObjectList(instance, InstanceConstants.FIELD_PORT_BINDINGS, PortInstance.class));
        this.primaryIp = DataAccessor.fieldString(instance, InstanceConstants.FIELD_PRIMARY_IP_ADDRESS);
        this.primaryMacAddress = DataAccessor.fieldString(instance, InstanceConstants.FIELD_PRIMARY_MAC_ADDRESSS);
        this.serviceId = instance.getServiceId();
        this.serviceIds = new HashSet<>(DataAccessor.fieldLongList(instance, InstanceConstants.FIELD_SERVICE_IDS));
        this.serviceIndex = instance.getServiceIndex();
        this.shouldRestart = DataAccessor.fieldBool(instance, InstanceConstants.FIELD_SHOULD_RESTART);
        this.stackId = instance.getStackId();
        this.startCount = instance.getStartCount();
        this.state = instance.getState();
        this.system = instance.getSystem();
        this.uuid = instance.getUuid();

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

    public List<Link> getLinks() {
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

    public Integer getExitCode() {
        return exitCode;
    }

    public boolean isShouldRestart() {
        return shouldRestart;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        InstanceInfo that = (InstanceInfo) o;

        if (id != that.id) return false;
        if (system != that.system) return false;
        if (shouldRestart != that.shouldRestart) return false;
        if (networkFromContainerId != null ? !networkFromContainerId.equals(that.networkFromContainerId) : that.networkFromContainerId != null)
            return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (hostname != null ? !hostname.equals(that.hostname) : that.hostname != null) return false;
        if (uuid != null ? !uuid.equals(that.uuid) : that.uuid != null) return false;
        if (healthState != null ? !healthState.equals(that.healthState) : that.healthState != null) return false;
        if (state != null ? !state.equals(that.state) : that.state != null) return false;
        if (externalId != null ? !externalId.equals(that.externalId) : that.externalId != null) return false;
        if (primaryIp != null ? !primaryIp.equals(that.primaryIp) : that.primaryIp != null) return false;
        if (primaryMacAddress != null ? !primaryMacAddress.equals(that.primaryMacAddress) : that.primaryMacAddress != null)
            return false;
        if (serviceIndex != null ? !serviceIndex.equals(that.serviceIndex) : that.serviceIndex != null) return false;
        if (exitCode != null ? !exitCode.equals(that.exitCode) : that.exitCode != null) return false;
        if (agentId != null ? !agentId.equals(that.agentId) : that.agentId != null) return false;
        if (serviceId != null ? !serviceId.equals(that.serviceId) : that.serviceId != null) return false;
        if (stackId != null ? !stackId.equals(that.stackId) : that.stackId != null) return false;
        if (hostId != null ? !hostId.equals(that.hostId) : that.hostId != null) return false;
        if (memoryReservation != null ? !memoryReservation.equals(that.memoryReservation) : that.memoryReservation != null)
            return false;
        if (milliCpuReservation != null ? !milliCpuReservation.equals(that.milliCpuReservation) : that.milliCpuReservation != null)
            return false;
        if (networkId != null ? !networkId.equals(that.networkId) : that.networkId != null) return false;
        if (startCount != null ? !startCount.equals(that.startCount) : that.startCount != null) return false;
        if (serviceIds != null ? !serviceIds.equals(that.serviceIds) : that.serviceIds != null) return false;
        if (ports != null ? !ports.equals(that.ports) : that.ports != null) return false;
        if (dns != null ? !dns.equals(that.dns) : that.dns != null) return false;
        if (dnsSearch != null ? !dnsSearch.equals(that.dnsSearch) : that.dnsSearch != null) return false;
        if (labels != null ? !labels.equals(that.labels) : that.labels != null) return false;
        if (links != null ? !links.equals(that.links) : that.links != null) return false;
        if (healthCheckHosts != null ? !healthCheckHosts.equals(that.healthCheckHosts) : that.healthCheckHosts != null)
            return false;
        return healthCheck != null ? healthCheck.equals(that.healthCheck) : that.healthCheck == null;
    }

    @Override
    public int hashCode() {
        int result = (int) (id ^ (id >>> 32));
        result = 31 * result + (networkFromContainerId != null ? networkFromContainerId.hashCode() : 0);
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (hostname != null ? hostname.hashCode() : 0);
        result = 31 * result + (uuid != null ? uuid.hashCode() : 0);
        result = 31 * result + (healthState != null ? healthState.hashCode() : 0);
        result = 31 * result + (state != null ? state.hashCode() : 0);
        result = 31 * result + (externalId != null ? externalId.hashCode() : 0);
        result = 31 * result + (primaryIp != null ? primaryIp.hashCode() : 0);
        result = 31 * result + (primaryMacAddress != null ? primaryMacAddress.hashCode() : 0);
        result = 31 * result + (serviceIndex != null ? serviceIndex.hashCode() : 0);
        result = 31 * result + (exitCode != null ? exitCode.hashCode() : 0);
        result = 31 * result + (system ? 1 : 0);
        result = 31 * result + (shouldRestart ? 1 : 0);
        result = 31 * result + (agentId != null ? agentId.hashCode() : 0);
        result = 31 * result + (serviceId != null ? serviceId.hashCode() : 0);
        result = 31 * result + (stackId != null ? stackId.hashCode() : 0);
        result = 31 * result + (hostId != null ? hostId.hashCode() : 0);
        result = 31 * result + (memoryReservation != null ? memoryReservation.hashCode() : 0);
        result = 31 * result + (milliCpuReservation != null ? milliCpuReservation.hashCode() : 0);
        result = 31 * result + (networkId != null ? networkId.hashCode() : 0);
        result = 31 * result + (startCount != null ? startCount.hashCode() : 0);
        result = 31 * result + (serviceIds != null ? serviceIds.hashCode() : 0);
        result = 31 * result + (ports != null ? ports.hashCode() : 0);
        result = 31 * result + (dns != null ? dns.hashCode() : 0);
        result = 31 * result + (dnsSearch != null ? dnsSearch.hashCode() : 0);
        result = 31 * result + (labels != null ? labels.hashCode() : 0);
        result = 31 * result + (links != null ? links.hashCode() : 0);
        result = 31 * result + (healthCheckHosts != null ? healthCheckHosts.hashCode() : 0);
        result = 31 * result + (healthCheck != null ? healthCheck.hashCode() : 0);
        return result;
    }
}
