package io.cattle.platform.agent.instance.factory;

import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.Instance;

public interface AgentInstanceFactory {

    Agent createAgent(Instance instance);

    boolean areAllCredentialsActive(Agent agent);

    void deleteAgent(Instance instance);

}
