package io.github.ibuildthecloud.dstack.process.common.handler;

import java.util.Map;

import io.github.ibuildthecloud.dstack.engine.handler.HandlerResult;
import io.github.ibuildthecloud.dstack.engine.process.ProcessInstance;
import io.github.ibuildthecloud.dstack.engine.process.ProcessState;
import io.github.ibuildthecloud.dstack.eventing.EventService;
import io.github.ibuildthecloud.dstack.eventing.model.Event;
import io.github.ibuildthecloud.dstack.eventing.model.EventVO;
import io.github.ibuildthecloud.dstack.object.ObjectManager;
import io.github.ibuildthecloud.dstack.object.util.ObjectUtils;
import io.github.ibuildthecloud.dstack.util.type.CollectionUtils;
import io.github.ibuildthecloud.dstack.util.type.NamedUtils;

import javax.inject.Inject;

public class EventBasedProcessHandler extends AbstractObjectProcessHandler {

    public static String DEFAULT_NAME = "EventBased";

    EventService eventService;
    String[] processNames;
    String eventName;
    Integer retry;
    Long timeoutMillis;

    public EventBasedProcessHandler() {
        if ( this.getClass() == EventBasedProcessHandler.class ) {
            setName(DEFAULT_NAME);
        }
    }

    @Override
    public String[] getProcessNames() {
        if ( DEFAULT_NAME.equals(getName()) ) {
            return new String[0];
        }
        if ( processNames == null ) {
            return new String[] { NamedUtils.toDotSeparated(getName()) };
        }
        return processNames;
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Object resource = state.getResource();
        String type = objectManager.getType(resource);
        if ( type == null ) {
            type = resource.getClass().getName();
        }

        String idString = null;
        Object id = ObjectUtils.getId(resource);

        if ( id != null ) {
            idString = id.toString();
        }

        Event request = EventVO
                            .newEvent(process.getName())
                            .resourceId(idString)
                            .resourceType(type)
                            .data(state.getData());

        Event response = eventService.callSync(request, retry, timeoutMillis);

        return postEvent(state, process, CollectionUtils.toMap(response.getData()));
    }

    protected HandlerResult postEvent(ProcessState state, ProcessInstance process, Map<Object,Object> data) {
        return new HandlerResult(data);
    }

    public EventService getEventService() {
        return eventService;
    }

    @Inject
    public void setEventService(EventService eventService) {
        this.eventService = eventService;
    }

    @Override
    public ObjectManager getObjectManager() {
        return objectManager;
    }

    @Override
    @Inject
    public void setObjectManager(ObjectManager objectManager) {
        this.objectManager = objectManager;
    }

    public void setProcessNames(String[] processNames) {
        this.processNames = processNames;
    }

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public Integer getRetry() {
        return retry;
    }

    public void setRetry(Integer retry) {
        this.retry = retry;
    }

    public Long getTimeoutMillis() {
        return timeoutMillis;
    }

    public void setTimeoutMillis(Long timeoutMillis) {
        this.timeoutMillis = timeoutMillis;
    }

}
