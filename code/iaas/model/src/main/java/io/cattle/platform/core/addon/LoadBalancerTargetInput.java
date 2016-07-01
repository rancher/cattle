package io.cattle.platform.core.addon;

import io.cattle.platform.core.constants.LoadBalancerConstants;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.ServiceConsumeMap;
import io.cattle.platform.core.model.ServiceExposeMap;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.util.DataAccessor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LoadBalancerTargetInput {
    Long instanceId;
    String ipAddress;
    String name;
    List<? extends String> ports = new ArrayList<>();
    Service service;

    public LoadBalancerTargetInput(Service service, ServiceExposeMap exposeMap, ServiceConsumeMap serviceLink,
            JsonMapper jsonMapper) {
        this.service = service;
        if (exposeMap != null) {
            this.instanceId = exposeMap.getInstanceId();
            this.ipAddress = exposeMap.getIpAddress();
            this.name = exposeMap.getUuid();
        }

        this.ports = DataAccessor.fields(serviceLink).
                withKey(LoadBalancerConstants.FIELD_LB_TARGET_PORTS).withDefault(Collections.EMPTY_LIST)
                .asList(jsonMapper, String.class);
    }

    public LoadBalancerTargetInput() {
    }

    public Long getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(Long instanceId) {
        this.instanceId = instanceId;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public void setPorts(List<? extends String> ports) {
        this.ports = ports;
    }

    public List<? extends String> getPorts() {
        return ports;
    }

    public String getName() {
        return name;
    }

    public Service getService() {
        return service;
    }

    public void setService(Service service) {
        this.service = service;
    }

    public void setName(String name) {
        this.name = name;
    }
}
