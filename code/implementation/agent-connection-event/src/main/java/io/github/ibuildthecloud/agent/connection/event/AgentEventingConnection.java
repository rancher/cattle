package io.github.ibuildthecloud.agent.connection.event;

import io.github.ibuildthecloud.agent.server.connection.AgentConnection;
import io.github.ibuildthecloud.dstack.async.utils.AsyncUtils;
import io.github.ibuildthecloud.dstack.eventing.EventCallOptions;
import io.github.ibuildthecloud.dstack.eventing.EventService;
import io.github.ibuildthecloud.dstack.eventing.model.Event;
import io.github.ibuildthecloud.dstack.eventing.model.EventVO;
import io.github.ibuildthecloud.dstack.iaas.event.IaasEvents;

import java.io.IOException;

import com.google.common.util.concurrent.ListenableFuture;

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
            return AsyncUtils.error(new IOException("Agent connection is closed"));
        }

        EventVO<Object> withAgentEvent = new EventVO<Object>(event);
        withAgentEvent.setName(IaasEvents.appendAgent(event.getName(), getAgentId()));

        return eventService.call(withAgentEvent, new EventCallOptions(0, event.getTimeoutMillis()));
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
