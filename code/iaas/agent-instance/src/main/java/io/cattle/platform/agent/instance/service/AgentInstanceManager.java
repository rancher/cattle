package io.cattle.platform.agent.instance.service;

import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.NetworkServiceProvider;
import io.cattle.platform.core.model.Nic;

import java.util.List;
import java.util.Map;

public interface AgentInstanceManager {

    List<? extends Agent> getAgents(NetworkServiceProvider provider);

    Map<NetworkServiceProvider,Instance> getAgentInstances(Nic nic);

}
