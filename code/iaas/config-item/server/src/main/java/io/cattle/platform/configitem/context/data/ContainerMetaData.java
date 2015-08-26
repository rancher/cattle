package io.cattle.platform.configitem.context.data;

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

import com.fasterxml.jackson.annotation.JsonIgnore;

public class ContainerMetaData {
    private Long serviceId;
    private HostMetaData hostMetaData;
    private String dnsPrefix;

    String name;
    String primary_ip;
    List<String> ips = new ArrayList<>();
    // host:public:private
    List<String> ports = new ArrayList<>();
    String service_name;
    String stack_name;
    Map<String, String> labels = new HashMap<>();

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
    public void setInstance(Instance instance) {
        this.name = instance.getName();
        Map<String, String> labels = DataAccessor.fields(instance).withKey(InstanceConstants.FIELD_LABELS)
                .withDefault(Collections.EMPTY_MAP).as(Map.class);
        this.labels = labels;
        List<String> portsObj = DataAccessor.fields(instance)
                .withKey(InstanceConstants.FIELD_PORTS).withDefault(Collections.EMPTY_LIST)
                .as(List.class);
        if (hostMetaData != null) {
            String hostIp = hostMetaData.agent_ip;
            if (hostIp == null) {
                ports.addAll(portsObj);
            } else {
                for (String portObj : portsObj) {
                    ports.add(hostIp + ":" + portObj);
                }
            }
        }
    }

    public void setService_name(String service_name) {
        this.service_name = service_name;
    }

    public void setStack_name(String stack_name) {
        this.stack_name = stack_name;
    }

    public void setHostMetaData(HostMetaData hostMetaData) {
        this.hostMetaData = hostMetaData;
    }

    public void setExposeMap(ServiceExposeMap exposeMap) {
        if (exposeMap != null) {
            this.dnsPrefix = exposeMap.getDnsPrefix();
            this.serviceId = exposeMap.getServiceId();
        }
    }

    @JsonIgnore
    public HostMetaData getHostMetaData() {
        return hostMetaData;
    }

    @JsonIgnore
    public Long getServiceId() {
        return serviceId;
    }

    @JsonIgnore
    public String getDnsPrefix() {
        return dnsPrefix;
    }

}
