package io.cattle.platform.metadata.model;

import io.cattle.platform.core.constants.NetworkConstants;
import io.cattle.platform.core.model.Network;
import io.cattle.platform.object.util.DataAccessor;

import java.util.Map;

public class NetworkInfo implements MetadataObject {

    String name;
    String uuid;
    boolean hostPorts;
    Map<String, Object> metadata;
    String defaultPolicyAction;
    Object policy;

    public NetworkInfo(Network network) {
        this.name = network.getName();
        this.uuid = network.getUuid();
        this.metadata = DataAccessor.fieldMap(network, NetworkConstants.FIELD_METADATA);
        this.defaultPolicyAction = DataAccessor.fieldString(network, NetworkConstants.FIELD_METADATA);
        this.policy = DataAccessor.fieldObject(network, NetworkConstants.FIELD_POLICY);
    }

    public String getName() {
        return name;
    }

    @Override
    public String getUuid() {
        return uuid;
    }

    public boolean isHostPorts() {
        return hostPorts;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public String getDefaultPolicyAction() {
        return defaultPolicyAction;
    }

    public Object getPolicy() {
        return policy;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((defaultPolicyAction == null) ? 0 : defaultPolicyAction.hashCode());
        result = prime * result + (hostPorts ? 1231 : 1237);
        result = prime * result + ((metadata == null) ? 0 : metadata.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((policy == null) ? 0 : policy.hashCode());
        result = prime * result + ((uuid == null) ? 0 : uuid.hashCode());
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
        NetworkInfo other = (NetworkInfo) obj;
        if (defaultPolicyAction == null) {
            if (other.defaultPolicyAction != null)
                return false;
        } else if (!defaultPolicyAction.equals(other.defaultPolicyAction))
            return false;
        if (hostPorts != other.hostPorts)
            return false;
        if (metadata == null) {
            if (other.metadata != null)
                return false;
        } else if (!metadata.equals(other.metadata))
            return false;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        if (policy == null) {
            if (other.policy != null)
                return false;
        } else if (!policy.equals(other.policy))
            return false;
        if (uuid == null) {
            if (other.uuid != null)
                return false;
        } else if (!uuid.equals(other.uuid))
            return false;
        return true;
    }

}
