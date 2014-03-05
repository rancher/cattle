package io.github.ibuildthecloud.agent.connection.simulator.impl;

import io.github.ibuildthecloud.agent.connection.simulator.AgentConnectionSimulator;
import io.github.ibuildthecloud.agent.connection.simulator.AgentSimulatorEventProcessor;
import io.github.ibuildthecloud.dstack.eventing.model.Event;
import io.github.ibuildthecloud.dstack.eventing.model.EventVO;
import io.github.ibuildthecloud.dstack.json.JsonMapper;
import io.github.ibuildthecloud.dstack.util.type.Priority;

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

        if ( eventString.contains(name + "::fail") ) {
            return EventVO.reply(event)
                    .withTransitioningMessage("Failing [" + name + "]")
                    .withTransitioning(Event.TRANSITIONING_ERROR);
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
