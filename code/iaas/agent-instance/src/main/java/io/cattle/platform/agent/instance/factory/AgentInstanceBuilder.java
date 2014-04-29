package io.cattle.platform.agent.instance.factory;

import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.NetworkServiceProvider;

public interface AgentInstanceBuilder {

    AgentInstanceBuilder forVnetId(long vnetId);

    AgentInstanceBuilder withImageUuid(String uuid);

    AgentInstanceBuilder withAccountId(Long accountId);

    AgentInstanceBuilder withZoneId(Long zoneId);

    AgentInstanceBuilder withAgentGroupId(Long agentGroupId);

    AgentInstanceBuilder withAccountOwned(boolean accountOwned);

    AgentInstanceBuilder withNetworkServiceProvider(NetworkServiceProvider networkServiceProvider);

    AgentInstanceBuilder withInstance(Instance instance);

    AgentInstanceBuilder withInstanceKind(String kind);

    AgentInstanceBuilder withManagedConfig(boolean managedConfig);

    AgentInstanceBuilder withPrivileged(boolean privileged);

    public Instance build();

}
