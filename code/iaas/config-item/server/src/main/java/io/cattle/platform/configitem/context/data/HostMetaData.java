package io.cattle.platform.configitem.context.data;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class HostMetaData {
    String agent_ip;
    String name;
    Map<String, String> labels;
    Long hostId;

    public String getAgent_ip() {
        return agent_ip;
    }

    public String getName() {
        return name;
    }

    public Map<String, String> getLabels() {
        return labels;
    }

    public HostMetaData(String agent_ip, String name, Map<String, String> labels, long hostId) {
        super();
        this.agent_ip = agent_ip;
        this.name = name;
        this.labels = labels;
        this.hostId = hostId;
    }

    @JsonIgnore
    public Long getHostId() {
        return hostId;
    }
}
