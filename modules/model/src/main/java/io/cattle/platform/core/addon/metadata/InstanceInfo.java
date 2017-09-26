package io.cattle.platform.core.addon.metadata;

import io.cattle.platform.core.addon.HealthcheckState;
import io.cattle.platform.core.addon.InstanceHealthCheck;
import io.cattle.platform.core.addon.Link;
import io.cattle.platform.core.addon.PortInstance;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.object.util.DataAccessor;
import io.github.ibuildthecloud.gdapi.annotation.Field;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class InstanceInfo implements MetadataObject {
    long id;
    String uuid;
    String environmentUuid;

    String name;
    String hostname;
    String healthState;
    String state;
    String externalId;
    String primaryIp;
    String primaryMacAddress;
    Integer serviceIndex;
    Integer exitCode;

    boolean shouldRestart;
    boolean nativeContainer;
    boolean desired;

    long accountId;
    Long agentId;
    Long serviceId;
    Long stackId;
    Long hostId;
    Long memoryReservation;
    Long milliCpuReservation;
    Long networkFromContainerId;
    Long networkId;
    Long startCount;
    Long createIndex;
    Long deploymentUnitId;

    Set<Long> serviceIds;
    Set<PortInstance> ports;
    List<String> portSpecs;
    List<String> dns;
    List<String> dnsSearch;
    Map<String, String> labels;
    List<Link> links;
    List<HealthcheckState> healthCheckHosts;
    HealthcheckInfo healthCheck;

    public InstanceInfo(Instance instance) {
        this.accountId = instance.getAccountId();
        this.agentId = instance.getAgentId();
        this.createIndex = instance.getCreateIndex();
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
        this.nativeContainer = instance.getNativeContainer();
        this.networkFromContainerId = instance.getNetworkContainerId();
        this.networkId = instance.getNetworkId();
        this.ports = new HashSet<>(DataAccessor.fieldObjectList(instance, InstanceConstants.FIELD_PORT_BINDINGS, PortInstance.class));
        this.portSpecs = DataAccessor.fieldStringList(instance, InstanceConstants.FIELD_PORTS);
        this.primaryIp = DataAccessor.fieldString(instance, InstanceConstants.FIELD_PRIMARY_IP_ADDRESS);
        this.primaryMacAddress = DataAccessor.fieldString(instance, InstanceConstants.FIELD_PRIMARY_MAC_ADDRESSS);
        this.serviceId = instance.getServiceId();
        this.serviceIds = new HashSet<>(DataAccessor.fieldLongList(instance, InstanceConstants.FIELD_SERVICE_IDS));
        this.serviceIndex = instance.getServiceIndex();
        this.shouldRestart = DataAccessor.fieldBool(instance, InstanceConstants.FIELD_SHOULD_RESTART);
        this.stackId = instance.getStackId();
        this.startCount = instance.getStartCount();
        this.state = instance.getState();
        this.uuid = instance.getUuid();

        InstanceHealthCheck hc = DataAccessor.field(instance, InstanceConstants.FIELD_HEALTH_CHECK, InstanceHealthCheck.class);
        if (hc != null) {
            this.healthCheck = new HealthcheckInfo(hc);
        }
        this.desired = instance.getDesired();
        this.deploymentUnitId = instance.getDeploymentUnitId();
    }

    @Override
    @Field(typeString = "reference[instance]")
    public Long getInfoTypeId() {
        return id;
    }

    public Long getCreateIndex() {
        return createIndex;
    }

    public boolean isDesired() {
        return desired;
    }

    @Field(include = false)
    public List<String> getPortSpecs() {
        return portSpecs;
    }

    public boolean isNativeContainer() {
        return nativeContainer;
    }

    @Override
    public String getEnvironmentUuid() {
        return environmentUuid;
    }

    @Override
    public String getInfoType() {
        return "instance";
    }

    @Override
    public void setEnvironmentUuid(String environmentUuid) {
        this.environmentUuid = environmentUuid;
    }

    @Field(typeString = "reference[account]")
    public long getAccountId() {
        return accountId;
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

    @Field(typeString = "reference[instance]")
    public Long getNetworkFromContainerId() {
        return networkFromContainerId;
    }

    @Field(typeString = "reference[service]")
    public Long getServiceId() {
        return serviceId;
    }

    @Field(typeString = "reference[stack]")
    public Long getStackId() {
        return stackId;
    }

    @Field(typeString = "reference[host]")
    public Long getHostId() {
        return hostId;
    }

    public Long getMemoryReservation() {
        return memoryReservation;
    }

    public Long getMilliCpuReservation() {
        return milliCpuReservation;
    }

    @Field(typeString = "reference[network]")
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

    @Field(typeString = "reference[agent]")
    public Long getAgentId() {
        return agentId;
    }

    public HealthcheckInfo getHealthCheck() {
        return healthCheck;
    }

    public Set<PortInstance> getPorts() {
        return ports;
    }

    @Field(typeString = "array[reference[service]]")
    public Set<Long> getServiceIds() {
        return serviceIds;
    }

    public Integer getExitCode() {
        return exitCode;
    }

    public boolean isShouldRestart() {
        return shouldRestart;
    }

    public boolean getDesired() {
        return desired;
    }

    @Field(typeString = "reference[deploymentUnit]")
    public Long getDeploymentUnitId() {
        return deploymentUnitId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        InstanceInfo that = (InstanceInfo) o;

        return new EqualsBuilder()
                .append(id, that.id)
                .append(shouldRestart, that.shouldRestart)
                .append(nativeContainer, that.nativeContainer)
                .append(desired, that.desired)
                .append(accountId, that.accountId)
                .append(uuid, that.uuid)
                .append(environmentUuid, that.environmentUuid)
                .append(name, that.name)
                .append(hostname, that.hostname)
                .append(healthState, that.healthState)
                .append(state, that.state)
                .append(externalId, that.externalId)
                .append(primaryIp, that.primaryIp)
                .append(primaryMacAddress, that.primaryMacAddress)
                .append(serviceIndex, that.serviceIndex)
                .append(exitCode, that.exitCode)
                .append(agentId, that.agentId)
                .append(serviceId, that.serviceId)
                .append(stackId, that.stackId)
                .append(hostId, that.hostId)
                .append(memoryReservation, that.memoryReservation)
                .append(milliCpuReservation, that.milliCpuReservation)
                .append(networkFromContainerId, that.networkFromContainerId)
                .append(networkId, that.networkId)
                .append(startCount, that.startCount)
                .append(createIndex, that.createIndex)
                .append(deploymentUnitId, that.deploymentUnitId)
                .append(serviceIds, that.serviceIds)
                .append(ports, that.ports)
                .append(portSpecs, that.portSpecs)
                .append(dns, that.dns)
                .append(dnsSearch, that.dnsSearch)
                .append(labels, that.labels)
                .append(links, that.links)
                .append(healthCheckHosts, that.healthCheckHosts)
                .append(healthCheck, that.healthCheck)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(id)
                .append(uuid)
                .append(environmentUuid)
                .append(name)
                .append(hostname)
                .append(healthState)
                .append(state)
                .append(externalId)
                .append(primaryIp)
                .append(primaryMacAddress)
                .append(serviceIndex)
                .append(exitCode)
                .append(shouldRestart)
                .append(nativeContainer)
                .append(desired)
                .append(accountId)
                .append(agentId)
                .append(serviceId)
                .append(stackId)
                .append(hostId)
                .append(memoryReservation)
                .append(milliCpuReservation)
                .append(networkFromContainerId)
                .append(networkId)
                .append(startCount)
                .append(createIndex)
                .append(deploymentUnitId)
                .append(serviceIds)
                .append(ports)
                .append(portSpecs)
                .append(dns)
                .append(dnsSearch)
                .append(labels)
                .append(links)
                .append(healthCheckHosts)
                .append(healthCheck)
                .toHashCode();
    }
}

