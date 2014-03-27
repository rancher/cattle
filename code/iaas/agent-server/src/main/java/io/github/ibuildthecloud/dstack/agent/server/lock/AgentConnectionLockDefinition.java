package io.github.ibuildthecloud.dstack.agent.server.lock;

import io.github.ibuildthecloud.dstack.core.model.Agent;
import io.github.ibuildthecloud.dstack.lock.definition.AbstractLockDefinition;

public class AgentConnectionLockDefinition extends AbstractLockDefinition {

    public AgentConnectionLockDefinition(Agent agent) {
        this(agent.getId());
    }

    public AgentConnectionLockDefinition(Long agentId) {
        super("AGENT.CONNECTION." + agentId);
    }

}
