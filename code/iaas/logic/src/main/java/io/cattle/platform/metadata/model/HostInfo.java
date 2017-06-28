package io.cattle.platform.metadata.model;

import io.cattle.platform.core.constants.HostConstants;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.object.util.DataAccessor;

import java.util.Map;

public class HostInfo implements MetadataObject {
    long id;
    String agentIp;
    String name;
    String state;
    String agentState;
    Map<String, String> labels;
    String uuid;
    String hostname;
    Long milliCpu;
    Long memory;

    public HostInfo(Host host) {
        this.id = host.getId();
        this.agentIp = DataAccessor.fieldString(host, HostConstants.FIELD_IP_ADDRESS);
        this.name = host.getName();
        this.labels = DataAccessor.getLabels(host);
        this.uuid = host.getUuid();
        this.hostname = DataAccessor.fieldString(host, HostConstants.FIELD_HOSTNAME);
        this.milliCpu = host.getMilliCpu();
        this.memory = host.getMemory();
        this.state = host.getState();
        this.agentState = host.getAgentState();
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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((agentIp == null) ? 0 : agentIp.hashCode());
        result = prime * result + ((agentState == null) ? 0 : agentState.hashCode());
        result = prime * result + ((hostname == null) ? 0 : hostname.hashCode());
        result = prime * result + (int) (id ^ (id >>> 32));
        result = prime * result + ((labels == null) ? 0 : labels.hashCode());
        result = prime * result + ((memory == null) ? 0 : memory.hashCode());
        result = prime * result + ((milliCpu == null) ? 0 : milliCpu.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((state == null) ? 0 : state.hashCode());
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
        HostInfo other = (HostInfo) obj;
        if (agentIp == null) {
            if (other.agentIp != null)
                return false;
        } else if (!agentIp.equals(other.agentIp))
            return false;
        if (agentState == null) {
            if (other.agentState != null)
                return false;
        } else if (!agentState.equals(other.agentState))
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
        if (memory == null) {
            if (other.memory != null)
                return false;
        } else if (!memory.equals(other.memory))
            return false;
        if (milliCpu == null) {
            if (other.milliCpu != null)
                return false;
        } else if (!milliCpu.equals(other.milliCpu))
            return false;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        if (state == null) {
            if (other.state != null)
                return false;
        } else if (!state.equals(other.state))
            return false;
        if (uuid == null) {
            if (other.uuid != null)
                return false;
        } else if (!uuid.equals(other.uuid))
            return false;
        return true;
    }

}