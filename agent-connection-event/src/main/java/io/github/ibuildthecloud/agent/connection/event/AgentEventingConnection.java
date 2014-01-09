package io.github.ibuildthecloud.agent.connection.event;

import java.io.IOException;

import io.github.ibuildthecloud.agent.server.connection.AgentConnection;
import io.github.ibuildthecloud.dstack.eventing.EventService;
import io.github.ibuildthecloud.dstack.eventing.model.Event;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

public class AgentEventingConnection implements AgentConnection {

    String uri;
    EventService eventService;
    long agentId;
    boolean open = true;

    public AgentEventingConnection(long agentId, String uri, EventService eventService) {
        super();
        this.agentId = agentId;
        this.uri = uri;
        this.eventService = eventService;
    }

    @Override
    public long getAgentId() {
        return agentId;
    }

    @Override
    public ListenableFuture<Event> execute(Event event) {
        if ( ! open ) {
            SettableFuture<Event> future = SettableFuture.create();
            future.setException(new IOException("Agent connection is closed"));
            return future;
        }

        return eventService.call(event, 0, 15000L);
    }

    @Override
    public void close() {
        open = false;
    }

    @Override
    public String getUri() {
        return uri;
    }

    @Override
    public boolean isOpen() {
        return open;
    }

}
