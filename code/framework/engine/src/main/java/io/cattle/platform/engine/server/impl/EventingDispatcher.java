package io.cattle.platform.engine.server.impl;

import io.cattle.platform.engine.eventing.EngineEvents;
import io.cattle.platform.engine.server.ProcessInstanceDispatcher;
import io.cattle.platform.eventing.EventService;
import io.cattle.platform.eventing.model.EventVO;

import javax.inject.Inject;

public class EventingDispatcher implements ProcessInstanceDispatcher {

    EventService eventService;

    @Override
    public void execute(Long id) {
        if (id != null) {
            eventService.publish(EventVO.newEvent(EngineEvents.PROCESS_EXECUTE).withResourceId(id.toString()));
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
