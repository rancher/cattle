package io.github.ibuildthecloud.dstack.agent.connection.simulator;

import io.github.ibuildthecloud.dstack.agent.server.connection.AgentConnection;
import io.github.ibuildthecloud.dstack.async.utils.AsyncUtils;
import io.github.ibuildthecloud.dstack.core.model.Agent;
import io.github.ibuildthecloud.dstack.eventing.model.Event;
import io.github.ibuildthecloud.dstack.eventing.model.EventVO;

import java.util.List;

import com.google.common.util.concurrent.ListenableFuture;

public class AgentConnectionSimulator implements AgentConnection {

    Agent agent;
    boolean open = true;
    List<AgentSimulatorEventProcessor> processors;

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
    public ListenableFuture<Event> execute(Event event) {
        for ( AgentSimulatorEventProcessor processor : processors ) {
            Event response;
            try {
                response = processor.handle(this, event);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
            if ( response != null ) {
                return AsyncUtils.done(response);
            }
        }
        return AsyncUtils.done((Event)EventVO.reply(event));
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

}
