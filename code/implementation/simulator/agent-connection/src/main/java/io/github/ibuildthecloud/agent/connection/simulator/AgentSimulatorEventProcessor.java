package io.github.ibuildthecloud.agent.connection.simulator;

import io.github.ibuildthecloud.dstack.eventing.model.Event;

public interface AgentSimulatorEventProcessor {

    Event handle(AgentConnectionSimulator simulator, Event event) throws Exception;

}
