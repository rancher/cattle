package io.cattle.platform.agent.server.lock;

import io.cattle.platform.core.model.Agent;
import io.cattle.platform.lock.definition.AbstractLockDefinition;

public class AgentConnectionLockDefinition extends AbstractLockDefinition {

    public AgentConnectionLockDefinition(Agent agent) {
        this(agent.getId());
    }

    public AgentConnectionLockDefinition(Long agentId) {
        super("AGENT.CONNECTION." + agentId);
    }

}
