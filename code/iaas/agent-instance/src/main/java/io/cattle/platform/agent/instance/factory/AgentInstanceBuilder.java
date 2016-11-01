package io.cattle.platform.agent.instance.factory;

import io.cattle.platform.core.model.Instance;

import java.util.Map;

public interface AgentInstanceBuilder {

    AgentInstanceBuilder withImageUuid(String uuid);

    AgentInstanceBuilder withAccountId(Long accountId);

    AgentInstanceBuilder withZoneId(Long zoneId);

    AgentInstanceBuilder withAccountOwned(boolean accountOwned);

    AgentInstanceBuilder withInstance(Instance instance);

    AgentInstanceBuilder withInstanceKind(String kind);

    AgentInstanceBuilder withPrivileged(boolean privileged);

    AgentInstanceBuilder withName(String name);

    AgentInstanceBuilder withUri(String uri);

    AgentInstanceBuilder withParameters(Map<String, Object> params);

    public Instance build();

}
