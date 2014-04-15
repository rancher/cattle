package io.cattle.platform.agent.instance.factory;

import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.NetworkService;

public interface AgentInstanceBuilder {

    AgentInstanceBuilder forVnetId(long vnetId);

    AgentInstanceBuilder withImageUuid(String uuid);

    AgentInstanceBuilder withAccountId(Long accountId);

    AgentInstanceBuilder withZoneId(Long zoneId);

    AgentInstanceBuilder withAgentGroupId(Long agentGroupId);

    AgentInstanceBuilder withAccountOwned(boolean accountOwned);

    AgentInstanceBuilder withNetworkService(NetworkService networkService);

    AgentInstanceBuilder withInstance(Instance instance);

    public Instance build();

}
