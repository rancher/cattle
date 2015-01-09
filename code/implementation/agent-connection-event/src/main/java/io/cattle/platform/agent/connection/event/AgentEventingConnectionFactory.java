package io.cattle.platform.agent.connection.event;

import io.cattle.platform.agent.server.connection.AgentConnection;
import io.cattle.platform.agent.server.connection.AgentConnectionFactory;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.eventing.EventService;

import java.io.IOException;

import javax.inject.Inject;

public class AgentEventingConnectionFactory implements AgentConnectionFactory {

    private static final String PROTOCOL = "event://";

    EventService eventService;

    @Override
    public AgentConnection createConnection(Agent agent) throws IOException {
        String uri = agent.getUri();
        if (uri == null || !uri.startsWith(PROTOCOL)) {
            return null;
        }

        return new AgentEventingConnection(agent.getId(), agent.getUri(), eventService);
    }

    public EventService getEventService() {
        return eventService;
    }

    @Inject
    public void setEventService(EventService eventService) {
        this.eventService = eventService;
    }

}
