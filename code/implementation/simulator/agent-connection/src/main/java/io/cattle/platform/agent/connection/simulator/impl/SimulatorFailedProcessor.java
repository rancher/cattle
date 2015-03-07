package io.cattle.platform.agent.connection.simulator.impl;

import io.cattle.platform.agent.connection.simulator.AgentConnectionSimulator;
import io.cattle.platform.agent.connection.simulator.AgentSimulatorEventProcessor;
import io.cattle.platform.eventing.model.Event;
import io.cattle.platform.eventing.model.EventVO;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.util.type.Priority;

import javax.inject.Inject;

public class SimulatorFailedProcessor implements AgentSimulatorEventProcessor, Priority {

    JsonMapper jsonMapper;

    @Override
    public int getPriority() {
        return Priority.PRE;
    }

    @Override
    public Event handle(AgentConnectionSimulator simulator, Event event) throws Exception {
        String name = event.getName();
        String eventString = jsonMapper.writeValueAsString(event);

        if (eventString.contains(name + "::fail")) {
            return EventVO.reply(event).withTransitioningMessage("Failing [" + name + "]").withTransitioning(Event.TRANSITIONING_ERROR);
        }

        return null;
    }

    public JsonMapper getJsonMapper() {
        return jsonMapper;
    }

    @Inject
    public void setJsonMapper(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

}
