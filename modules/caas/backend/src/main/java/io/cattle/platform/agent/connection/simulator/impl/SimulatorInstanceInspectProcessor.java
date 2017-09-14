package io.cattle.platform.agent.connection.simulator.impl;

import io.cattle.platform.agent.connection.simulator.AgentConnectionSimulator;
import io.cattle.platform.agent.connection.simulator.AgentSimulatorEventProcessor;
import io.cattle.platform.eventing.model.Event;
import io.cattle.platform.eventing.model.EventVO;
import io.cattle.platform.framework.event.FrameworkEvents;
import io.cattle.platform.util.type.CollectionUtils;

import java.util.HashMap;
import java.util.Map;

public class SimulatorInstanceInspectProcessor implements AgentSimulatorEventProcessor {

    @Override
    public Event handle(AgentConnectionSimulator simulator, Event event) throws Exception {
        if (!FrameworkEvents.INSPECT.equals(event.getName())) {
            return null;
        }

        Map<String, Object> inner = new HashMap<>();
        inner.put("instanceInspect", null);
        return EventVO.reply(event).withData(CollectionUtils.asMap("data", inner));
    }

}
