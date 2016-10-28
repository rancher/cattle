package io.cattle.platform.agent.instance.factory.impl;

import io.cattle.platform.agent.instance.factory.AgentInstanceBuilder;
import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.NetworkServiceProviderConstants;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.NetworkServiceProvider;
import io.cattle.platform.core.util.SystemLabels;
import io.cattle.platform.object.util.DataAccessor;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.netflix.config.DynamicStringProperty;

public class AgentInstanceBuilderImpl implements AgentInstanceBuilder {

    private static final DynamicStringProperty DEFAULT_IMAGE_UUID = ArchaiusUtil.getString("agent.instance.image.uuid");
    private static final DynamicStringProperty DEFAULT_NAME = ArchaiusUtil.getString("agent.instance.name");

    String instanceKind = InstanceConstants.KIND_CONTAINER;
    Long vnetId;
    Long accountId;
    Long zoneId;
    Long agentGroupId;
    Long resourceAccountId;
    NetworkServiceProvider networkServiceProvider;
    String imageUuid;
    String name = DEFAULT_NAME.get();
    String instanceTriggeredStop = InstanceConstants.ON_STOP_RESTART;
    boolean accountOwned = true;
    boolean managedConfig = false;
    boolean privileged = false;
    AgentInstanceFactoryImpl factory;
    String uri;
    Map<String, Object> params = new HashMap<>();
    String systemContainerType;
    Set<String> requestedRoles;

    public AgentInstanceBuilderImpl(AgentInstanceFactoryImpl factory) {
        super();
        this.factory = factory;
    }

    public AgentInstanceBuilderImpl(AgentInstanceFactoryImpl factory, Instance instance, Set<String> roles) {
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
        this.requestedRoles = roles;
    }

    @Override
    public AgentInstanceBuilderImpl forVnetId(long vnetId) {
        this.vnetId = vnetId;
        return this;
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
    public AgentInstanceBuilder withManagedConfig(boolean managedConfig) {
        this.managedConfig = managedConfig;
        return this;
    }

    @Override
    public AgentInstanceBuilder withInstanceTriggeredStop(String instanceTriggeredStop) {
        this.instanceTriggeredStop = instanceTriggeredStop;
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
    public AgentInstanceBuilder withNetworkServiceProvider(NetworkServiceProvider networkServiceProvider) {
        this.networkServiceProvider = networkServiceProvider;
        withImageUuid(DataAccessor.fields(networkServiceProvider).withKey(NetworkServiceProviderConstants.FIELD_AGENT_INSTANCE_IMAGE_UUID).withDefault(
                getImageUuid()).as(String.class));

        withAccountOwned(DataAccessor.fields(networkServiceProvider).withKey(NetworkServiceProviderConstants.FIELD_AGENT_ACCOUNT_OWNED).withDefault(
                isAccountOwned()).as(Boolean.class));

        return this;
    }

    @Override
    public AgentInstanceBuilder withInstance(Instance instance) {
        withAccountId(instance.getAccountId());
        withZoneId(instance.getZoneId());
        withAgentGroupId(factory.getAgentGroupId(instance));

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
    public AgentInstanceBuilder withAgentGroupId(Long agentGroupId) {
        this.agentGroupId = agentGroupId;
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

    public Long getAgentGroupId() {
        return agentGroupId;
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

    public NetworkServiceProvider getNetworkServiceProvider() {
        return networkServiceProvider;
    }

    public boolean isPrivileged() {
        return privileged;
    }

    public void setPrivileged(boolean privileged) {
        this.privileged = privileged;
    }

    public String getInstanceTriggeredStop() {
        return instanceTriggeredStop;
    }

    public void setInstanceTriggeredStop(String instanceTriggeredStop) {
        this.instanceTriggeredStop = instanceTriggeredStop;
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

    public Set<String> getRequestedRoles() {
        return requestedRoles;
    }

    @Override
    public AgentInstanceBuilder withSystemContainerType(String systemContainerType) {
        this.systemContainerType = systemContainerType;
        return this;
    }

    public String getSystemContainerType() {
        return systemContainerType;
    }

    public Long getResourceAccountId() {
        return resourceAccountId;
    }

    public void setResourceAccountId(Long resourceAccountId) {
        this.resourceAccountId = resourceAccountId;
    }

}
