package io.cattle.platform.configitem.context.data.metadata.common;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class HostMetaData {
    String agent_ip;
    String name;
    Map<String, String> labels;
    Long hostId;
    String uuid;

    public String getAgent_ip() {
        return agent_ip;
    }

    public String getName() {
        return name;
    }

    public Map<String, String> getLabels() {
        return labels;
    }

    public HostMetaData() {

    }

    public HostMetaData(String agent_ip, String name, Map<String, String> labels, long hostId, String uuid) {
        super();
        this.agent_ip = agent_ip;
        this.name = name;
        this.labels = labels;
        this.hostId = hostId;
        this.uuid = uuid;
    }

    @JsonIgnore
    public Long getHostId() {
        return hostId;
    }

    public String getUuid() {
        return uuid;
    }

    public void setAgent_ip(String agent_ip) {
        this.agent_ip = agent_ip;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setLabels(Map<String, String> labels) {
        this.labels = labels;
    }

    public void setHostId(Long hostId) {
        this.hostId = hostId;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
}
