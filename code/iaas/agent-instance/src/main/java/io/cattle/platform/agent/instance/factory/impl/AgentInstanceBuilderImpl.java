package io.cattle.platform.agent.instance.factory.impl;

import io.cattle.platform.agent.instance.factory.AgentInstanceBuilder;
import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.util.SystemLabels;
import io.cattle.platform.object.util.DataAccessor;

import java.util.HashMap;
import java.util.Map;

import com.netflix.config.DynamicStringProperty;

public class AgentInstanceBuilderImpl implements AgentInstanceBuilder {

    private static final DynamicStringProperty DEFAULT_IMAGE_UUID = ArchaiusUtil.getString("agent.instance.image.uuid");
    private static final DynamicStringProperty DEFAULT_NAME = ArchaiusUtil.getString("agent.instance.name");

    String instanceKind = InstanceConstants.KIND_CONTAINER;
    Long vnetId;
    Long accountId;
    Long zoneId;
    Long resourceAccountId;
    String imageUuid;
    String name = DEFAULT_NAME.get();
    boolean accountOwned = true;
    boolean managedConfig = false;
    boolean privileged = false;
    Map<String, Object> accountData = null;
    AgentInstanceFactoryImpl factory;
    String uri;
    Map<String, Object> params = new HashMap<>();

    public AgentInstanceBuilderImpl(AgentInstanceFactoryImpl factory) {
        super();
        this.factory = factory;
    }

    public AgentInstanceBuilderImpl(AgentInstanceFactoryImpl factory, Instance instance, Map<String, Object> accountData) {
        this(factory);
        this.accountId = instance.getAccountId();
        this.zoneId = instance.getZoneId();
        String uriPrefix = "event";
        Map<String, Object> labels = DataAccessor.fieldMap(instance, InstanceConstants.FIELD_LABELS);
        Object prefix = labels.get(SystemLabels.LABEL_AGENT_URI_PREFIX);
        if (prefix != null) {
            uriPrefix = prefix.toString();
        }
        this.uri = uriPrefix + ":///instanceId=" + instance.getId();
        this.resourceAccountId = instance.getAccountId();
        this.accountData = accountData;
    }

    @Override
    public AgentInstanceBuilder withImageUuid(String uuid) {
        this.imageUuid = uuid;
        return this;
    }

    @Override
    public AgentInstanceBuilder withInstanceKind(String kind) {
        this.instanceKind = kind;
        return this;
    }

    @Override
    public AgentInstanceBuilder withName(String name) {
        this.name = name;
        return this;
    }

    @Override
    public Instance build() {
        return factory.build(this);
    }

    public Long getVnetId() {
        return vnetId;
    }

    public String getImageUuid() {
        return imageUuid == null ? DEFAULT_IMAGE_UUID.get() : imageUuid;
    }

    @Override
    public AgentInstanceBuilder withInstance(Instance instance) {
        withAccountId(instance.getAccountId());
        withZoneId(instance.getZoneId());

        return this;
    }

    @Override
    public AgentInstanceBuilder withAccountId(Long accountId) {
        this.accountId = accountId;
        return this;
    }

    @Override
    public AgentInstanceBuilder withZoneId(Long zoneId) {
        this.zoneId = zoneId;
        return this;
    }

    @Override
    public AgentInstanceBuilder withAccountOwned(boolean accountOwned) {
        this.accountOwned = accountOwned;
        return this;
    }

    @Override
    public AgentInstanceBuilder withPrivileged(boolean privileged) {
        this.privileged = privileged;
        return this;
    }

    public Long getAccountId() {
        return accountId;
    }

    public Long getZoneId() {
        return zoneId;
    }

    public boolean isAccountOwned() {
        return accountOwned;
    }

    public AgentInstanceFactoryImpl getFactory() {
        return factory;
    }

    public String getInstanceKind() {
        return instanceKind;
    }

    public boolean isManagedConfig() {
        return managedConfig;
    }

    public boolean isPrivileged() {
        return privileged;
    }

    public void setPrivileged(boolean privileged) {
        this.privileged = privileged;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public AgentInstanceBuilder withUri(String uri) {
        this.uri = uri;
        return this;
    }

    public String getUri() {
        return uri;
    }

    @Override
    public AgentInstanceBuilder withParameters(Map<String, Object> params) {
        this.params = params;
        return this;
    }

    public Map<String, Object> getParams() {
        return params;
    }

    public Map<String, Object> getAccountData() {
        return accountData;
    }

    public Long getResourceAccountId() {
        return resourceAccountId;
    }

    public void setResourceAccountId(Long resourceAccountId) {
        this.resourceAccountId = resourceAccountId;
    }

}
