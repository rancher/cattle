package io.github.ibuildthecloud.agent.server.util;

import io.github.ibuildthecloud.agent.server.lock.AgentConnectionLockDefinition;
import io.github.ibuildthecloud.dstack.core.model.Agent;
import io.github.ibuildthecloud.dstack.lock.definition.LockDefinition;

public class AgentConnectionUtils {

    public static LockDefinition getConnectionLock(Agent agent) {
        return new AgentConnectionLockDefinition(agent);
    }

    public static LockDefinition getConnectionLock(Long agentId) {
        return new AgentConnectionLockDefinition(agentId);
    }

}
