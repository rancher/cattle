package io.cattle.platform.agent.server.eventing.impl;

import io.cattle.platform.agent.server.eventing.AgentEventListener;
import io.cattle.platform.agent.server.service.AgentService;
import io.cattle.platform.eventing.model.Event;

import javax.inject.Inject;

public class AgentEventListenerImpl implements AgentEventListener {

    AgentService agentService;

    @Override
    public void agentRequest(Event event) {
        agentService.execute(event);
    }

    public AgentService getAgentService() {
        return agentService;
    }

    @Inject
    public void setAgentService(AgentService agentService) {
        this.agentService = agentService;
    }

}
