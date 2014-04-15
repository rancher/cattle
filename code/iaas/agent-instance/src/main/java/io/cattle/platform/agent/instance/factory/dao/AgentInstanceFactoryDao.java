package io.cattle.platform.agent.instance.factory.dao;

import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.Instance;

public interface AgentInstanceFactoryDao {

    Agent getAgentByUri(String uri);

    Instance getInstanceByAgent(Agent agent);

}
