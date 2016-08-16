package io.cattle.platform.agent.connection.simulator;

import io.cattle.platform.core.model.Agent;
import io.cattle.platform.eventing.model.Event;
import io.cattle.platform.eventing.model.EventVO;
import io.cattle.platform.object.ObjectManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AgentConnectionSimulator implements Simulator {

    Agent agent;
    boolean open = true;
    ObjectManager objectManager;
    List<AgentSimulatorEventProcessor> processors;
    Map<String, Object[]> instances = new HashMap<String, Object[]>();

    public AgentConnectionSimulator(ObjectManager objectManager, Agent agent, List<AgentSimulatorEventProcessor> processors) {
        super();
        this.objectManager = objectManager;
        this.agent = agent;
        this.processors = processors;
    }

    @Override
    public Event execute(Event event) {
        objectManager.reload(agent);
        for (AgentSimulatorEventProcessor processor : processors) {
            Event response;
            try {
                response = processor.handle(this, event);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
            if (response != null) {
                return response;
            }
        }
        return EventVO.reply(event);
    }
    
    public Agent getAgent() {
        return agent;
    }

    public Map<String, Object[]> getInstances() {
        return instances;
    }

    public List<AgentSimulatorEventProcessor> getProcessors() {
        return processors;
    }

}
