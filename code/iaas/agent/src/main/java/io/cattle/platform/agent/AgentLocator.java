package io.cattle.platform.agent;

import io.cattle.platform.core.model.Agent;

import java.util.Map;

public interface AgentLocator {

    RemoteAgent lookupAgent(Object resource);

    Agent getAgentForDelegate(long agentId, Map<String, Object> instanceDataOut);

}
