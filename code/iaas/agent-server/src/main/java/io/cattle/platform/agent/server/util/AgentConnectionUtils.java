package io.cattle.platform.agent.server.util;

import io.cattle.platform.agent.server.lock.AgentConnectionLockDefinition;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.lock.definition.LockDefinition;

public class AgentConnectionUtils {

    public static LockDefinition getConnectionLock(Agent agent) {
        return new AgentConnectionLockDefinition(agent);
    }

}
