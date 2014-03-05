package io.github.ibuildthecloud.dstack.api.handler;

import io.github.ibuildthecloud.dstack.archaius.util.ArchaiusUtil;
import io.github.ibuildthecloud.dstack.deferred.util.DeferredUtils;
import io.github.ibuildthecloud.dstack.eventing.EventService;
import io.github.ibuildthecloud.dstack.eventing.model.EventVO;
import io.github.ibuildthecloud.dstack.framework.event.FrameworkEvents;
import io.github.ibuildthecloud.dstack.util.exception.ExceptionUtils;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.model.Schema.Method;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.handler.ApiRequestHandler;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.servlet.ServletException;

import com.netflix.config.DynamicStringListProperty;

public class EventNotificationHandler implements ApiRequestHandler {

    private static final DynamicStringListProperty EXCLUDE = ArchaiusUtil.getList("api.event.change.exclude.types");
    EventService eventService;
    Set<String> excludeTypes = new HashSet<String>();

    @Override
    public void handle(ApiRequest request) throws IOException {
        if ( Method.GET.isMethod(request.getMethod()) ) {
            return;
        }

        if ( excludeTypes.contains(request.getType()) ) {
            return;
        }

        Map<String,Object> data = new HashMap<String,Object>();
        data.put("method", request.getMethod().toString());
        data.put("id", request.getId());
        data.put("type", request.getType());
        data.put("action", request.getAction());
        data.put("responseCode", request.getResponseCode());

        DeferredUtils.deferPublish(eventService, EventVO.newEvent(FrameworkEvents.API_CHANGE)
                .withResourceId(request.getId())
                .withResourceType(request.getType())
                .withData(data));
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

        DeferredUtils.deferPublish(eventService, EventVO.newEvent(FrameworkEvents.API_EXCEPTION)
                .withData(data));

        return false;
    }

    @PostConstruct
    public void init() {
        load();
        EXCLUDE.addCallback(new Runnable() {
            @Override
            public void run() {
                load();
            }
        });
    }

    public void load() {
        excludeTypes = new HashSet<String>();
        excludeTypes.addAll(EXCLUDE.get());
    }

    public EventService getEventService() {
        return eventService;
    }

    @Inject
    public void setEventService(EventService eventService) {
        this.eventService = eventService;
    }

}
