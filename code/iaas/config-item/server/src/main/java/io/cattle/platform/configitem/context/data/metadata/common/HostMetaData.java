package io.cattle.platform.configitem.context.data.metadata.common;

import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.object.util.DataAccessor;

import java.util.Collections;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class HostMetaData {
    String agent_ip;
    String name;
    Map<String, String> labels;
    Long host_id;
    String uuid;
    String hostname;
    Long milli_cpu;
    Long memory;
    Long local_storage_mb;

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
        this.host_id = host.getId();
        this.uuid = host.getUuid();

        this.local_storage_mb = host.getLocalStorageMb();
        this.memory = host.getMemory();
        this.milli_cpu = host.getMilliCpu();
    }

    @JsonIgnore
    public Long getHost_id() {
        return host_id;
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

    public void setHost_id(Long hostId) {
        this.host_id = hostId;
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

    public Long getMilli_cpu() {
        return milli_cpu;
    }

    public void setMilli_cpu(Long milliCpu) {
        this.milli_cpu = milliCpu;
    }

    public Long getMemory() {
        return memory;
    }

    public void setMemory(Long memory) {
        this.memory = memory;
    }

    public Long getLocal_storage_mb() {
        return local_storage_mb;
    }

    public void setLocal_storage_mb(Long localStorageMb) {
        this.local_storage_mb = localStorageMb;
    }
}
