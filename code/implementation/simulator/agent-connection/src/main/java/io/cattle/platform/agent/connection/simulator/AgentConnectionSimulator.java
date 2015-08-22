package io.cattle.platform.agent.connection.simulator;

import io.cattle.platform.agent.server.connection.AgentConnection;
import io.cattle.platform.async.utils.AsyncUtils;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.eventing.EventProgress;
import io.cattle.platform.eventing.model.Event;
import io.cattle.platform.eventing.model.EventVO;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.util.concurrent.ListenableFuture;

public class AgentConnectionSimulator implements AgentConnection {

    Agent agent;
    boolean open = true;
    List<AgentSimulatorEventProcessor> processors;
    Map<String, String[]> instances = new HashMap<String, String[]>();

    public AgentConnectionSimulator(Agent agent, List<AgentSimulatorEventProcessor> processors) {
        super();
        this.agent = agent;
        this.processors = processors;
    }

    @Override
    public long getAgentId() {
        return agent.getId();
    }

    @Override
    public String getUri() {
        return agent.getUri();
    }

    @Override
    public ListenableFuture<Event> execute(Event event, EventProgress process) {
        for (AgentSimulatorEventProcessor processor : processors) {
            Event response;
            try {
                response = processor.handle(this, event);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
            if (response != null) {
                return AsyncUtils.done(response);
            }
        }
        return AsyncUtils.done((Event) EventVO.reply(event));
    }

    @Override
    public void close() {
        open = false;
    }

    @Override
    public boolean isOpen() {
        return open;
    }

    public Agent getAgent() {
        return agent;
    }

    public Map<String, String[]> getInstances() {
        return instances;
    }

    public List<AgentSimulatorEventProcessor> getProcessors() {
        return processors;
    }

}
