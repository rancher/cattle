package io.cattle.platform.core.addon.metadata;

import io.cattle.platform.core.constants.NetworkConstants;
import io.cattle.platform.core.model.Network;
import io.cattle.platform.object.util.DataAccessor;
import io.github.ibuildthecloud.gdapi.annotation.Field;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.Map;

public class NetworkInfo implements MetadataObject {

    Long id;
    String name;
    String uuid;
    String kind;
    String environmentUuid;
    boolean hostPorts;
    Map<String, Object> metadata;
    String defaultPolicyAction;
    Object policy;
    boolean isDefault;

    public NetworkInfo(Network network) {
        this.defaultPolicyAction = DataAccessor.fieldString(network, NetworkConstants.FIELD_DEFAULT_POLICY_ACTION);
        this.id = network.getId();
        this.kind = network.getKind();
        this.metadata = DataAccessor.fieldMap(network, NetworkConstants.FIELD_METADATA);
        this.name = network.getName();
        this.policy = DataAccessor.fieldObject(network, NetworkConstants.FIELD_POLICY);
        this.uuid = network.getUuid();
        this.isDefault = network.getIsDefault();
        this.hostPorts = DataAccessor.fieldBool(network, NetworkConstants.FIELD_HOST_PORTS);
    }

    public String getKind() {
        return kind;
    }

    @Override
    @Field(typeString = "reference[network]")
    public Long getInfoTypeId() {
        return id;
    }

    public Long getId() {
        return id;
    }

    public boolean isDefault() {
        return isDefault;
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

        return new EqualsBuilder()
                .append(hostPorts, that.hostPorts)
                .append(isDefault, that.isDefault)
                .append(id, that.id)
                .append(name, that.name)
                .append(uuid, that.uuid)
                .append(kind, that.kind)
                .append(environmentUuid, that.environmentUuid)
                .append(metadata, that.metadata)
                .append(defaultPolicyAction, that.defaultPolicyAction)
                .append(policy, that.policy)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(id)
                .append(name)
                .append(uuid)
                .append(kind)
                .append(environmentUuid)
                .append(hostPorts)
                .append(metadata)
                .append(defaultPolicyAction)
                .append(policy)
                .append(isDefault)
                .toHashCode();
    }
}
