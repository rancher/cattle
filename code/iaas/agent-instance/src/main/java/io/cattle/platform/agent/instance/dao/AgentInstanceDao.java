package io.cattle.platform.agent.instance.dao;

import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.Instance;

import java.util.List;

public interface AgentInstanceDao {

    Agent getAgentByUri(String uri);

    Instance getInstanceByAgent(Long agentId);

    boolean areAllCredentialsActive(Agent agent);

    List<Long> getAgentProvider(String providedServiceLabel, long accountId);

    List<Long> getAgentProviderIgnoreHealth(String providedServiceLabel, long accountId);
}
