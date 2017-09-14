package io.cattle.platform.agent.connection.simulator;

import io.cattle.platform.eventing.model.Event;

public interface AgentSimulatorEventProcessor {

    Event handle(AgentConnectionSimulator simulator, Event event) throws Exception;

}
