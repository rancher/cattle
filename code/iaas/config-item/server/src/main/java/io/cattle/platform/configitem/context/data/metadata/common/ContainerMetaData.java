package io.cattle.platform.configitem.context.data.metadata.common;

import io.cattle.platform.configitem.context.dao.MetaDataInfoDao;
import io.cattle.platform.configitem.context.dao.MetaDataInfoDao.Version;
import io.cattle.platform.configitem.context.data.metadata.version2.ContainerMetaDataVersion3;
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

    protected String name;
    String uuid;
    String primary_ip;
    List<String> ips = new ArrayList<>();
    // host:public:private
    List<String> ports = new ArrayList<>();
    protected String service_name;
    protected String stack_name;
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
    Long memory_reservation;
    Long milli_cpu_reservation;

    public ContainerMetaData(ContainerMetaData that) {
        this.name = that.name;
        this.uuid = that.uuid;
        this.primary_ip = that.primary_ip;
        this.ips = that.ips;
        this.ports = that.ports;
        this.service_name = that.service_name;
        this.stack_name = that.stack_name;
        this.stack_uuid = that.stack_uuid;
        this.labels = that.labels;
        this.create_index = that.create_index;
        this.host_uuid = that.host_uuid;
        this.hostname = that.hostname;
        this.health_state = that.health_state;
        this.start_count = that.start_count;
        this.service_index = that.service_index;
        this.state = that.state;
        this.external_id = that.external_id;
        this.primary_mac_address = that.primary_mac_address;
        this.memory_reservation = that.memory_reservation;
        this.milli_cpu_reservation = that.milli_cpu_reservation;
        this.serviceId = that.serviceId;
        this.hostMetaData = that.hostMetaData;
        this.dnsPrefix = that.dnsPrefix;
    }


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
        this.name = instance.getName();
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
        this.memory_reservation = instance.getMemoryReservation();
        this.milli_cpu_reservation = instance.getMilliCpuReservation();
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

    public Long getMemory_reservation() {
        return memory_reservation;
    }

    public void setMemory_reservation(Long memory) {
        this.memory_reservation = memory;
    }

    public Long getMilli_cpu_reservation() {
        return milli_cpu_reservation;
    }

    public void setMilli_cpu_reservation(Long milli_cpu_reservation) {
        this.milli_cpu_reservation = milli_cpu_reservation;
    }

    public static ContainerMetaData getContainerMetaData(ContainerMetaData data, Version version) {
        if (version == MetaDataInfoDao.Version.version1 || version == MetaDataInfoDao.Version.version2) {
            return new ContainerMetaData(data);
        } else {
            return new ContainerMetaDataVersion3(data);
        }
    }
}
