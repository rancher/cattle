package io.cattle.platform.agent.connection.simulator.impl;

import io.cattle.platform.agent.connection.simulator.AgentConnectionSimulator;
import io.cattle.platform.agent.connection.simulator.AgentSimulatorEventProcessor;
import io.cattle.platform.eventing.model.Event;
import io.cattle.platform.eventing.model.EventVO;
import io.cattle.platform.iaas.event.IaasEvents;
import io.cattle.platform.util.type.CollectionUtils;

public class SimulatorConsoleProcessor implements AgentSimulatorEventProcessor {

    @Override
    public Event handle(AgentConnectionSimulator simulator, Event event) throws Exception {
        if (!IaasEvents.CONSOLE_ACCESS.equals(event.getName())) {
            return null;
        }

        return EventVO.reply(event).withData(CollectionUtils.asMap("kind", "fake", "url", "http://localhost/console"));
    }

}
