package io.github.ibuildthecloud.dstack.engine.server.impl;

import io.github.ibuildthecloud.dstack.engine.eventing.EngineEvents;
import io.github.ibuildthecloud.dstack.engine.server.ProcessInstanceDispatcher;
import io.github.ibuildthecloud.dstack.engine.server.ProcessServer;
import io.github.ibuildthecloud.dstack.eventing.EventService;
import io.github.ibuildthecloud.dstack.eventing.util.EventUtils;

import javax.inject.Inject;

public class EventingDispatcher implements ProcessInstanceDispatcher {

    EventService eventService;

    @Override
    public void execute(ProcessServer server, Long id) {
        if ( id != null ) {
            eventService.publish(EventUtils.newEvent(EngineEvents.PROCESS_EXECUTE, id.toString()));
        }
    }

    public EventService getEventService() {
        return eventService;
    }

    @Inject
    public void setEventService(EventService eventService) {
        this.eventService = eventService;
    }

}
