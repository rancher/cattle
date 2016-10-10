package io.cattle.platform.configitem.context.data.metadata.common;

import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Nic;
import io.cattle.platform.core.model.ServiceExposeMap;
import io.cattle.platform.core.util.PortSpec;
import io.cattle.platform.object.util.DataAccessor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

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
    String stack_uuid;
    Map<String, String> labels = new HashMap<>();
    Long create_index;
    String host_uuid;
    String hostname;
    String health_state;
    Long start_count;
    String service_index;
    String state;
    String external_id;
    String primary_mac_address;
    Long memory;


    public ContainerMetaData() {
    }

    public String getName() {
        return name;
    }

    public String getPrimary_ip() {
        return primary_ip;
    }

    public String getExternal_id() {
        return external_id;
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

    public String getStack_uuid() {
        return stack_uuid;
    }

    public void setStack_uuid(String stack_uuid) {
        this.stack_uuid = stack_uuid;
    }

    public Map<String, String> getLabels() {
        return labels;
    }

    public void setIp(String ip) {
        this.primary_ip = ip;
        this.ips.add(primary_ip);
    }

    @SuppressWarnings("unchecked")
    public void setInstanceAndHostMetadata(Instance instance, HostMetaData hostMetaData) {
        this.hostMetaData = hostMetaData;
        if (instance.getName() != null) {
            this.name = instance.getName().toLowerCase();
        }
        this.uuid = instance.getUuid();
        this.external_id = instance.getExternalId();
        Map<String, String> labels = DataAccessor.fields(instance).withKey(InstanceConstants.FIELD_LABELS)
                .withDefault(Collections.EMPTY_MAP).as(Map.class);
        this.labels = labels;
        List<String> portsObj = DataAccessor.fields(instance)
                .withKey(InstanceConstants.FIELD_PORTS).withDefault(Collections.EMPTY_LIST)
                .as(List.class);
        this.hostname = instance.getHostname();
        if (hostMetaData != null) {
            this.host_uuid = hostMetaData.getUuid();
            String hostIp = hostMetaData.agent_ip;
            if (hostIp == null) {
                ports.addAll(portsObj);
            } else {
                for (String portObj : portsObj) {
                    PortSpec port = new PortSpec(portObj);
                    if (StringUtils.isEmpty(port.getIpAddress())) {
                        ports.add(hostIp + ":" + portObj);
                    } else {
                        ports.add(portObj);
                    }
                }
            }
        }
        this.create_index = instance.getCreateIndex();
        this.health_state = instance.getHealthState();
        this.start_count = instance.getStartCount();
        this.state = instance.getState();
        this.memory = DataAccessor.fieldLong(instance, "memory");

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

    public void setNicInformation(Nic nic) {
        if(nic != null) {
            this.primary_mac_address = nic.getMacAddress();
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

    public void setExternal_id(String external_id) {
        this.external_id = external_id;
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

    public String getService_index() {
        return service_index;
    }

    public void setService_index(String service_index) {
        this.service_index = service_index;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getPrimary_mac_address() {
        return primary_mac_address;
    }

    public void setPrimary_mac_address(String mac_address) {
        this.primary_mac_address = mac_address;
    }

    public Long getMemory() {
        return memory;
    }

    public void setMemory(Long memory) {
        this.memory = memory;
    }
}
