package io.github.ibuildthecloud.agent.server.lock;

import io.github.ibuildthecloud.dstack.core.model.Agent;
import io.github.ibuildthecloud.dstack.lock.definition.AbstractLockDefinition;

public class AgentConnectionLockDefinition extends AbstractLockDefinition {

    public AgentConnectionLockDefinition(Agent agent) {
        super("AGENT.CONNECTION." + agent.getId());
    }

}
