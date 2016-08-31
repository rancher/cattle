package io.cattle.platform.process.instance;

import io.cattle.platform.agent.RemoteAgent;
import io.cattle.platform.core.constants.AgentConstants;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.eventing.EventCallOptions;
import io.cattle.platform.eventing.model.Event;
import io.cattle.platform.process.common.handler.AgentBasedProcessHandler;

public class IgnoreReconnectionAgentHandler extends AgentBasedProcessHandler {

    @Override
    protected Event callSync(RemoteAgent remoteAgent, Event event, EventCallOptions options) {
        Agent agent = loadResource(Agent.class, remoteAgent.getAgentId());
        if (agent != null && (AgentConstants.STATE_RECONNECTING.equals(agent.getState()) ||
                AgentConstants.STATE_DISCONNECTED.equals(agent.getState()))) {
            return null;
        }
        return super.callSync(remoteAgent, event, options);
    }

}
