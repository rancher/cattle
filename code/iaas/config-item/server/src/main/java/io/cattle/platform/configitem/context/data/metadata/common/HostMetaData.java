package io.cattle.platform.configitem.context.data.metadata.common;

import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.util.type.CollectionUtils;

import java.util.Collections;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class HostMetaData {
    String agent_ip;
    String name;
    Map<String, String> labels;
    Long hostId;
    String uuid;
    String hostname;
    Long memory;

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

    @SuppressWarnings("unchecked")
    public HostMetaData(String agent_ip, Host host) {
        super();
        this.agent_ip = agent_ip;
        String hostname = DataAccessor.fieldString(host, "hostname");
        this.name = StringUtils.isEmpty(host.getName()) ?  hostname: host.getName();
        this.hostname = hostname;
        this.labels = (Map<String, String>) DataAccessor.fields(host)
                .withKey(InstanceConstants.FIELD_LABELS)
                .withDefault(Collections.EMPTY_MAP).as(Map.class);
        this.hostId = host.getId();
        this.uuid = host.getUuid();
        Object obj = CollectionUtils.getNestedValue(DataAccessor.fieldMap(host, "info"), "memoryInfo", "memTotal");
        if (obj != null && obj instanceof Number) {
            this.memory = ((Number)obj).longValue() * 1024 * 1024;
        }
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

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public Long getMemory() {
        return memory;
    }

    public void setMemory(Long memory) {
        this.memory = memory;
    }
}
