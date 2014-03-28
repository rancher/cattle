package io.cattle.platform.agent.util;

import io.cattle.platform.core.constants.AgentConstants;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.framework.event.Ping;

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
