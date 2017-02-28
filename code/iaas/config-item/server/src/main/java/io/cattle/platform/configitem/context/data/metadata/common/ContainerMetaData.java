package io.cattle.platform.configitem.context.data.metadata.common;

import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.object.util.DataAccessor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.IteratorUtils;

import com.google.common.base.Splitter;

public class ContainerMetaData {
    String name;
    String uuid;
    String primary_ip;
    List<String> ips = new ArrayList<>();
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
    Long memory_reservation;
    Long milli_cpu_reservation;
    String network_uuid;
    String network_from_container_uuid;
    Boolean system;
    List<String> dns = new ArrayList<>();
    List<String> dns_search = new ArrayList<>();
    List<String> health_check_hosts = new ArrayList<>(); // list of hostUUIDs
    String environment_uuid;
    Map<String, String> links = new HashMap<>(); // container links where key is linkName, value is instanceUUID
    String host_ip;
    // helper field needed by metadata service to process object
    String metadata_kind;

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

    public Map<String, String> getLabels() {
        return labels;
    }

    public void setIp(String ip) {
        this.primary_ip = ip;
        this.ips.add(primary_ip);
    }

    @SuppressWarnings("unchecked")
    public void setInstanceAndHostMetadata(Instance instance, HostMetaData host, List<String> healthcheckHosts,
            Account account) {
        this.name = instance.getName();
        this.uuid = instance.getUuid();
        this.external_id = instance.getExternalId();
        this.system = instance.getSystem();
        Map<String, String> labels = DataAccessor.fields(instance).withKey(InstanceConstants.FIELD_LABELS)
                .withDefault(Collections.EMPTY_MAP).as(Map.class);
        this.labels = labels;
        this.hostname = instance.getHostname();
        this.ports = DataAccessor.fields(instance)
                .withKey(InstanceConstants.FIELD_PORTS).withDefault(Collections.EMPTY_LIST)
                .as(List.class);
        if (host != null) {
            this.host_uuid = host.uuid;
            this.host_ip = host.agent_ip;
        }
        this.create_index = instance.getCreateIndex();
        this.health_state = instance.getHealthState();
        this.start_count = instance.getStartCount();
        this.state = instance.getState();
        this.memory_reservation = instance.getMemoryReservation();
        this.milli_cpu_reservation = instance.getMilliCpuReservation();
        if (instance.getDnsInternal() != null) {
            this.dns = IteratorUtils.toList(Splitter.on(",").omitEmptyStrings().trimResults()
                    .split(instance.getDnsInternal()).iterator());
        }
        if (instance.getDnsSearchInternal() != null) {
            this.dns_search = IteratorUtils.toList(Splitter.on(",").omitEmptyStrings().trimResults()
                    .split(instance.getDnsSearchInternal()).iterator());
        }
        if (healthcheckHosts != null) {
            this.health_check_hosts = healthcheckHosts;
        }
        this.environment_uuid = account.getUuid();
        this.metadata_kind = "container";
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

    public String getNetwork_uuid() {
        return network_uuid;
    }

    public void setNetwork_uuid(String network_uuid) {
        this.network_uuid = network_uuid;
    }

    public String getNetwork_from_container_uuid() {
        return network_from_container_uuid;
    }

    public void setNetwork_from_container_uuid(String network_from_container_uuid) {
        this.network_from_container_uuid = network_from_container_uuid;
    }

    public Boolean getSystem() {
        return system;
    }

    public void setSystem(Boolean system) {
        this.system = system;
    }

    public List<String> getDns() {
        return dns;
    }

    public void setDns(List<String> dns) {
        this.dns = dns;
    }

    public List<String> getDns_search() {
        return dns_search;
    }

    public void setDns_search(List<String> dns_search) {
        this.dns_search = dns_search;
    }

    public List<String> getHealth_check_hosts() {
        return health_check_hosts;
    }

    public void setHealth_check_hosts(List<String> health_check_hosts) {
        this.health_check_hosts = health_check_hosts;
    }

    public String getEnvironment_uuid() {
        return environment_uuid;
    }

    public void setEnvironment_uuid(String environment_uuid) {
        this.environment_uuid = environment_uuid;
    }

    public String getMetadata_kind() {
        return metadata_kind;
    }

    public void setMetadata_kind(String metadata_kind) {
        this.metadata_kind = metadata_kind;
    }

    public String getHost_ip() {
        return host_ip;
    }

    public void setHost_ip(String host_ip) {
        this.host_ip = host_ip;
    }

    public String getService_name() {
        return service_name;
    }

    public void setService_name(String service_name) {
        this.service_name = service_name;
    }

    public String getStack_name() {
        return stack_name;
    }

    public void setStack_name(String stack_name) {
        this.stack_name = stack_name;
    }

    public String getStack_uuid() {
        return stack_uuid;
    }

    public void setStack_uuid(String stack_uuid) {
        this.stack_uuid = stack_uuid;
    }
}
