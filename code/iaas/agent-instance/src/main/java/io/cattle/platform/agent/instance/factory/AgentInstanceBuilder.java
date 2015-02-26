package io.cattle.platform.agent.instance.factory;

import io.cattle.platform.core.constants.InstanceConstants.SystemContainer;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.NetworkServiceProvider;

import java.util.Map;

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

    AgentInstanceBuilder withInstanceTriggeredStop(String instanceTriggeredStop);

    AgentInstanceBuilder withName(String name);

    AgentInstanceBuilder withUri(String uri);

    AgentInstanceBuilder withParameters(Map<String, Object> params);

    AgentInstanceBuilder withSystemContainerType(SystemContainer systemContainerType);

    public Instance build();

}
