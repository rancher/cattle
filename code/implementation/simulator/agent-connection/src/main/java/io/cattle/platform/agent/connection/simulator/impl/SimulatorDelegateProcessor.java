package io.cattle.platform.agent.connection.simulator.impl;

import io.cattle.platform.agent.connection.simulator.AgentConnectionSimulator;
import io.cattle.platform.agent.connection.simulator.AgentSimulatorEventProcessor;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.eventing.model.Event;
import io.cattle.platform.eventing.model.EventVO;
import io.cattle.platform.iaas.event.IaasEvents;
import io.cattle.platform.iaas.event.delegate.DelegateEvent;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.util.type.Priority;

import javax.inject.Inject;

public class SimulatorDelegateProcessor implements AgentSimulatorEventProcessor, Priority {

    @Inject
    JsonMapper jsonMapper;
    @Inject
    ObjectManager objectManager;

    @Override
    public int getPriority() {
        return Priority.PRE;
    }

    @Override
    public Event handle(AgentConnectionSimulator simulator, final Event event) throws Exception {
        String name = event.getName();

        if (name == null || !name.startsWith(IaasEvents.DELEGATE_REQUEST)) {
            return null;
        }

        DelegateEvent delegate = jsonMapper.convertValue(event, DelegateEvent.class);
        Object agentId = delegate.getData().getInstanceData().get("agentId");

        if (agentId == null) {
            throw new IllegalStateException("Failed to find agent id to simulate for delegate event [" + event.getId() + "]");
        }

        Agent agent = objectManager.loadResource(Agent.class, agentId.toString());
        if (agent == null) {
            throw new IllegalStateException("Failed to find agent to simulate for delegate event [" + event.getId() + "]");
        }

        AgentConnectionSimulator delegateSimulator = new AgentConnectionSimulator(objectManager, agent, simulator.getProcessors());

        Event input = delegateSimulator.execute(delegate.getData().getEvent());
        return EventVO.reply(event).withData(input);
    }
}
