package io.github.ibuildthecloud.dstack.agent.util;

import io.github.ibuildthecloud.dstack.core.constants.AgentConstants;
import io.github.ibuildthecloud.dstack.core.model.Agent;
import io.github.ibuildthecloud.dstack.framework.event.Ping;

public class AgentUtils {

    public static Ping newPing(Agent agent) {
        if ( agent == null ) {
            return null;
        }
        return newPing(agent.getId());
    }

    public static Ping newPing(long agentId) {
        Ping ping = new Ping();
        ping.setResourceType(AgentConstants.TYPE);
        ping.setResourceId(Long.toString(agentId));

        return ping;
    }
}
