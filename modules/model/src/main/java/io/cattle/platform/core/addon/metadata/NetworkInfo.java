package io.cattle.platform.core.addon.metadata;

import io.cattle.platform.core.constants.NetworkConstants;
import io.cattle.platform.core.model.Network;
import io.cattle.platform.object.util.DataAccessor;

import java.util.Map;

public class NetworkInfo implements MetadataObject {

    Long id;
    String name;
    String uuid;
    String environmentUuid;
    boolean hostPorts;
    Map<String, Object> metadata;
    String defaultPolicyAction;
    Object policy;

    public NetworkInfo(Network network) {
        this.id = network.getId();
        this.name = network.getName();
        this.uuid = network.getUuid();
        this.metadata = DataAccessor.fieldMap(network, NetworkConstants.FIELD_METADATA);
        this.defaultPolicyAction = DataAccessor.fieldString(network, NetworkConstants.FIELD_METADATA);
        this.policy = DataAccessor.fieldObject(network, NetworkConstants.FIELD_POLICY);
    }

    public Long getId() {
        return id;
    }

    @Override
    public String getEnvironmentUuid() {
        return environmentUuid;
    }

    @Override
    public String getInfoType() {
        return "network";
    }

    @Override
    public void setEnvironmentUuid(String environmentUuid) {
        this.environmentUuid = environmentUuid;
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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        NetworkInfo that = (NetworkInfo) o;

        if (hostPorts != that.hostPorts) return false;
        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (uuid != null ? !uuid.equals(that.uuid) : that.uuid != null) return false;
        if (environmentUuid != null ? !environmentUuid.equals(that.environmentUuid) : that.environmentUuid != null)
            return false;
        if (metadata != null ? !metadata.equals(that.metadata) : that.metadata != null) return false;
        if (defaultPolicyAction != null ? !defaultPolicyAction.equals(that.defaultPolicyAction) : that.defaultPolicyAction != null)
            return false;
        return policy != null ? policy.equals(that.policy) : that.policy == null;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (uuid != null ? uuid.hashCode() : 0);
        result = 31 * result + (environmentUuid != null ? environmentUuid.hashCode() : 0);
        result = 31 * result + (hostPorts ? 1 : 0);
        result = 31 * result + (metadata != null ? metadata.hashCode() : 0);
        result = 31 * result + (defaultPolicyAction != null ? defaultPolicyAction.hashCode() : 0);
        result = 31 * result + (policy != null ? policy.hashCode() : 0);
        return result;
    }
}
