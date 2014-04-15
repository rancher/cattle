package io.cattle.platform.agent.instance.factory.impl;

import com.netflix.config.DynamicStringProperty;

import io.cattle.platform.agent.instance.factory.AgentInstanceBuilder;
import io.cattle.platform.agent.instance.factory.impl.AgentInstanceFactoryImpl;
import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.constants.NetworkServiceConstants;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.NetworkService;
import io.cattle.platform.object.util.DataAccessor;

public class AgentInstanceBuilderImpl implements AgentInstanceBuilder {

    private static final DynamicStringProperty DEFAULT_IMAGE_UUID = ArchaiusUtil.getString("agent.instance.image.uuid");

    Long vnetId;
    Long accountId;
    Long zoneId;
    Long agentGroupId;
    String imageUuid;
    boolean accountOwned = false;
    AgentInstanceFactoryImpl factory;

    public AgentInstanceBuilderImpl(AgentInstanceFactoryImpl factory) {
        super();
        this.factory = factory;
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
    public AgentInstanceBuilder withNetworkService(NetworkService networkService) {
        withImageUuid(DataAccessor
                .fields(networkService)
                .withKey(NetworkServiceConstants.FIELD_AGENT_INSTANCE_IMAGE_UUID)
                .withDefault(getImageUuid())
                .as(String.class));

        withAccountOwned(DataAccessor
                .fields(networkService)
                .withKey(NetworkServiceConstants.FIELD_AGENT_ACCOUNT_OWNED)
                .withDefault(isAccountOwned())
                .as(Boolean.class));

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
}
