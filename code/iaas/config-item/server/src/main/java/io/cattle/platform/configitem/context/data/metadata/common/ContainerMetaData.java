package io.cattle.platform.configitem.context.data.metadata.common;

import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.IpAddress;
import io.cattle.platform.core.model.ServiceExposeMap;
import io.cattle.platform.object.util.DataAccessor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ContainerMetaData {
    private Long serviceId;
    private HostMetaData hostMetaData;
    private String dnsPrefix;

    String name;
    String uuid;
    String primary_ip;
    List<String> ips = new ArrayList<>();
    // host:public:private
    List<String> ports = new ArrayList<>();
    String service_name;
    String stack_name;
    Map<String, String> labels = new HashMap<>();
    Long create_index;
    String host_uuid;
    String hostname;
    String health_state;
    Long start_count;

    public ContainerMetaData() {
    }

    public String getName() {
        return name;
    }

    public String getPrimary_ip() {
        return primary_ip;
    }

    public List<String> getIps() {
        return ips;
    }

    public List<String> getPorts() {
        return ports;
    }

    public String getService_name() {
        return service_name;
    }

    public String getStack_name() {
        return stack_name;
    }

    public Map<String, String> getLabels() {
        return labels;
    }

    public void setIp(IpAddress ip) {
        this.primary_ip = ip.getAddress();
        this.ips.add(primary_ip);
    }

    @SuppressWarnings("unchecked")
    public void setInstanceAndHostMetadata(Instance instance, HostMetaData hostMetaData) {
        this.hostMetaData = hostMetaData;
        this.name = instance.getName();
        this.uuid = instance.getUuid();
        Map<String, String> labels = DataAccessor.fields(instance).withKey(InstanceConstants.FIELD_LABELS)
                .withDefault(Collections.EMPTY_MAP).as(Map.class);
        this.labels = labels;
        List<String> portsObj = DataAccessor.fields(instance)
                .withKey(InstanceConstants.FIELD_PORTS).withDefault(Collections.EMPTY_LIST)
                .as(List.class);
        if (hostMetaData != null) {
            this.host_uuid = hostMetaData.getUuid();
            this.hostname = hostMetaData.getName();
            String hostIp = hostMetaData.agent_ip;
            if (hostIp == null) {
                ports.addAll(portsObj);
            } else {
                for (String portObj : portsObj) {
                    ports.add(hostIp + ":" + portObj);
                }
            }
        }
        this.create_index = instance.getCreateIndex();
        this.health_state = instance.getHealthState();
        this.start_count = instance.getStartCount();
    }

    public void setService_name(String service_name) {
        this.service_name = service_name;
    }

    public void setStack_name(String stack_name) {
        this.stack_name = stack_name;
    }

    public void setExposeMap(ServiceExposeMap exposeMap) {
        if (exposeMap != null) {
            this.dnsPrefix = exposeMap.getDnsPrefix();
            this.serviceId = exposeMap.getServiceId();
        }
    }

    public HostMetaData getHostMetaData() {
        return hostMetaData;
    }

    public Long getServiceId() {
        return serviceId;
    }

    public String getDnsPrefix() {
        return dnsPrefix;
    }

    public Long getCreate_index() {
        return create_index;
    }

    public String getHost_uuid() {
        return host_uuid;
    }

    public String getHostname() {
        return hostname;
    }

    public String getUuid() {
        return uuid;
    }

    public String getHealth_state() {
        return health_state;
    }

    public Long getStart_count() {
        return start_count;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public void setPrimary_ip(String primary_ip) {
        this.primary_ip = primary_ip;
    }

    public void setIps(List<String> ips) {
        this.ips = ips;
    }

    public void setPorts(List<String> ports) {
        this.ports = ports;
    }

    public void setLabels(Map<String, String> labels) {
        this.labels = labels;
    }

    public void setCreate_index(Long create_index) {
        this.create_index = create_index;
    }

    public void setHost_uuid(String host_uuid) {
        this.host_uuid = host_uuid;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public void setHealth_state(String health_state) {
        this.health_state = health_state;
    }

    public void setStart_count(Long start_count) {
        this.start_count = start_count;
    }

}
