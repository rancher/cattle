package io.github.ibuildthecloud.dstack.agent.server.eventing.impl;

import io.github.ibuildthecloud.dstack.agent.server.eventing.AgentEventListener;
import io.github.ibuildthecloud.dstack.agent.server.service.AgentService;
import io.github.ibuildthecloud.dstack.eventing.model.Event;

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
