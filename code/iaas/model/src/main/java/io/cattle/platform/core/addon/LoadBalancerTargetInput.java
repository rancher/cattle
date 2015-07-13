package io.cattle.platform.core.addon;

import io.cattle.platform.core.constants.LoadBalancerConstants;
import io.cattle.platform.core.model.LoadBalancerTarget;
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
    List<? extends String> ports = new ArrayList<>();

    public LoadBalancerTargetInput(Long instanceId, String ipAddress, List<? extends String> ports) {
        super();
        this.instanceId = instanceId;
        this.ipAddress = ipAddress;
        if (ports != null) {
            this.ports = ports;
        }
    }

    public LoadBalancerTargetInput(LoadBalancerTarget target, JsonMapper jsonMapper) {
        this(target.getInstanceId(), target.getIpAddress(), DataAccessor.fields(target).
                withKey(LoadBalancerConstants.FIELD_LB_TARGET_PORTS).withDefault(Collections.EMPTY_LIST)
                .asList(jsonMapper, String.class));
    }

    public LoadBalancerTargetInput(ServiceExposeMap instanceToAdd, ServiceConsumeMap serviceLink, JsonMapper jsonMapper) {
        this(instanceToAdd.getInstanceId(), instanceToAdd.getIpAddress(), DataAccessor.fields(serviceLink).
                withKey(LoadBalancerConstants.FIELD_LB_TARGET_PORTS).withDefault(Collections.EMPTY_LIST)
                .asList(jsonMapper, String.class));
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

    public List<? extends String> getPorts() {
        return ports;
    }

    public void setPorts(List<String> ports) {
        this.ports = ports;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((instanceId == null) ? 0 : instanceId.hashCode());
        result = prime * result + ((ipAddress == null) ? 0 : ipAddress.hashCode());
        result = prime * result + ((ports == null) ? 0 : ports.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        LoadBalancerTargetInput other = (LoadBalancerTargetInput) obj;
        if (instanceId == null) {
            if (other.instanceId != null)
                return false;
        } else if (!instanceId.equals(other.instanceId))
            return false;
        if (ipAddress == null) {
            if (other.ipAddress != null)
                return false;
        } else if (!ipAddress.equals(other.ipAddress))
            return false;
        if (ports == null) {
            if (other.ports != null)
                return false;
        } else if (!ports.equals(other.ports))
            return false;
        return true;
    }
}
