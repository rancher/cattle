package io.github.ibuildthecloud.dstack.api.handler;

import io.github.ibuildthecloud.dstack.core.event.CoreEvents;
import io.github.ibuildthecloud.dstack.deferred.util.DeferredUtils;
import io.github.ibuildthecloud.dstack.eventing.EventService;
import io.github.ibuildthecloud.dstack.eventing.util.EventUtils;
import io.github.ibuildthecloud.dstack.util.exception.ExceptionUtils;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.model.Schema.Method;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.handler.ApiRequestHandler;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.servlet.ServletException;

public class EventNotificationHandler implements ApiRequestHandler {

    EventService eventService;

    @Override
    public void handle(ApiRequest request) throws IOException {
        if ( Method.GET.isMethod(request.getMethod()) ) {
            return;
        }

        Map<String,Object> data = new HashMap<String,Object>();
        data.put("method", request.getMethod().toString());
        data.put("id", request.getId());
        data.put("type", request.getType());
        data.put("action", request.getAction());
        data.put("responseCode", request.getResponseCode());

        DeferredUtils.deferPublish(eventService, EventUtils.newEventFromData(CoreEvents.API_CHANGE, data));
    }

    @Override
    public boolean handleException(ApiRequest request, Throwable t) throws IOException, ServletException {
        if ( t instanceof ClientVisibleException ) {
            return false;
        }

        Map<String,Object> data = new HashMap<String,Object>();
        data.put("method", request.getMethod().toString());
        data.put("id", request.getId());
        data.put("type", request.getType());
        data.put("action", request.getAction());

        data.put("message", t.getMessage());
        data.put("stackTrace", ExceptionUtils.toString(t));

        DeferredUtils.deferPublish(eventService, EventUtils.newEventFromData(CoreEvents.API_EXCEPTION, data));

        return false;
    }

    public EventService getEventService() {
        return eventService;
    }

    @Inject
    public void setEventService(EventService eventService) {
        this.eventService = eventService;
    }

}
