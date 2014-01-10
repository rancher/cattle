package io.github.ibuildthecloud.agent.connection.simulator;

import io.github.ibuildthecloud.agent.server.connection.AgentConnection;
import io.github.ibuildthecloud.dstack.core.model.Agent;
import io.github.ibuildthecloud.dstack.eventing.model.Event;
import io.github.ibuildthecloud.dstack.eventing.model.EventVO;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

public class AgentConnectionSimulator implements AgentConnection {

    Agent agent;
    boolean open = true;

    public AgentConnectionSimulator(Agent agent) {
        super();
        this.agent = agent;
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
        SettableFuture<Event> response = SettableFuture.create();
        response.set(EventVO.reply(event));

        return response;
    }

    @Override
    public void close() {
        open = false;
    }

    @Override
    public boolean isOpen() {
        return open;
    }

}
