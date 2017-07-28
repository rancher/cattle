package io.cattle.platform.metadata.model;

import io.cattle.platform.core.addon.PortInstance;
import io.cattle.platform.core.constants.HostConstants;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.object.util.DataAccessor;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class HostInfo implements MetadataObject {
    long id;
    Long agentId;
    String agentIp;
    String name;
    String state;
    String agentState;
    Map<String, String> labels;
    Set<PortInstance> ports;
    String uuid;
    String hostname;
    Long milliCpu;
    Long memory;

    public HostInfo(Host host) {
        this.id = host.getId();
        this.agentId = host.getAgentId();
        this.agentIp = DataAccessor.fieldString(host, HostConstants.FIELD_IP_ADDRESS);
        this.name = host.getName();
        this.labels = DataAccessor.getLabels(host);
        this.uuid = host.getUuid();
        this.hostname = DataAccessor.fieldString(host, HostConstants.FIELD_HOSTNAME);
        this.milliCpu = host.getMilliCpu();
        this.memory = host.getMemory();
        this.state = host.getState();
        this.agentState = host.getAgentState() == null ? this.state : host.getAgentState();
        this.ports = new HashSet<>(
                DataAccessor.fieldObjectList(host, HostConstants.FIELD_PUBLIC_ENDPOINTS, PortInstance.class));
    }

    public long getId() {
        return id;
    }

    public String getAgentIp() {
        return agentIp;
    }

    public String getName() {
        return name;
    }

    public Map<String, String> getLabels() {
        return labels;
    }

    @Override
    public String getUuid() {
        return uuid;
    }

    public String getHostname() {
        return hostname;
    }

    public Long getMilliCpu() {
        return milliCpu;
    }

    public Long getMemory() {
        return memory;
    }

    public String getState() {
        return state;
    }

    public String getAgentState() {
        return agentState;
    }

    public Set<PortInstance> getPorts() {
        return ports;
    }

    public Long getAgentId() {

        return agentId;
    }

    public void setAgentId(Long agentId) {
        this.agentId = agentId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        HostInfo hostInfo = (HostInfo) o;

        if (id != hostInfo.id) return false;
        if (agentId != null ? !agentId.equals(hostInfo.agentId) : hostInfo.agentId != null) return false;
        if (agentIp != null ? !agentIp.equals(hostInfo.agentIp) : hostInfo.agentIp != null) return false;
        if (name != null ? !name.equals(hostInfo.name) : hostInfo.name != null) return false;
        if (state != null ? !state.equals(hostInfo.state) : hostInfo.state != null) return false;
        if (agentState != null ? !agentState.equals(hostInfo.agentState) : hostInfo.agentState != null) return false;
        if (labels != null ? !labels.equals(hostInfo.labels) : hostInfo.labels != null) return false;
        if (ports != null ? !ports.equals(hostInfo.ports) : hostInfo.ports != null) return false;
        if (uuid != null ? !uuid.equals(hostInfo.uuid) : hostInfo.uuid != null) return false;
        if (hostname != null ? !hostname.equals(hostInfo.hostname) : hostInfo.hostname != null) return false;
        if (milliCpu != null ? !milliCpu.equals(hostInfo.milliCpu) : hostInfo.milliCpu != null) return false;
        return memory != null ? memory.equals(hostInfo.memory) : hostInfo.memory == null;
    }

    @Override
    public int hashCode() {
        int result = (int) (id ^ (id >>> 32));
        result = 31 * result + (agentId != null ? agentId.hashCode() : 0);
        result = 31 * result + (agentIp != null ? agentIp.hashCode() : 0);
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (state != null ? state.hashCode() : 0);
        result = 31 * result + (agentState != null ? agentState.hashCode() : 0);
        result = 31 * result + (labels != null ? labels.hashCode() : 0);
        result = 31 * result + (ports != null ? ports.hashCode() : 0);
        result = 31 * result + (uuid != null ? uuid.hashCode() : 0);
        result = 31 * result + (hostname != null ? hostname.hashCode() : 0);
        result = 31 * result + (milliCpu != null ? milliCpu.hashCode() : 0);
        result = 31 * result + (memory != null ? memory.hashCode() : 0);
        return result;
    }

}