package io.cattle.platform.agent.instance.factory;

import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.Instance;

public interface AgentInstanceFactory {

    Agent createAgent(Instance instance);

    void deleteAgent(Instance instance);

}
