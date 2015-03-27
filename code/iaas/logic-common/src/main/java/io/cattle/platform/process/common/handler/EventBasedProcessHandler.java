package io.cattle.platform.process.common.handler;

import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.eventing.EventCallOptions;
import io.cattle.platform.eventing.EventProgress;
import io.cattle.platform.eventing.EventService;
import io.cattle.platform.eventing.model.Event;
import io.cattle.platform.eventing.model.EventVO;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.util.ObjectUtils;
import io.cattle.platform.util.exception.ExecutionException;
import io.cattle.platform.util.type.CollectionUtils;
import io.cattle.platform.util.type.NamedUtils;
import io.cattle.platform.util.type.Priority;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang.StringUtils;

public class EventBasedProcessHandler extends AbstractObjectProcessHandler implements Priority {

    public static String DEFAULT_NAME = "EventBased";

    EventService eventService;
    String[] processNames;
    String eventName;
    Integer retry;
    Long timeoutMillis;
    String onError;
    int priority = Priority.SPECIFIC;

    public EventBasedProcessHandler(EventService eventService, ObjectManager objectManager, ObjectProcessManager objectProcessManager,
            ObjectMetaDataManager objectMetaDataManager) {
        this();
        this.eventService = eventService;
        this.objectManager = objectManager;
        this.objectProcessManager = objectProcessManager;
        this.objectMetaDataManager = objectMetaDataManager;
    }

    public EventBasedProcessHandler() {
        if (this.getClass() == EventBasedProcessHandler.class) {
            setName(DEFAULT_NAME);
        }
    }

    @Override
    public String[] getProcessNames() {
        if (DEFAULT_NAME.equals(getName())) {
            return new String[0];
        }
        if (processNames == null) {
            return new String[] { NamedUtils.toDotSeparated(getName()) };
        }
        return processNames;
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        final Object resource = state.getResource();
        String type = objectManager.getType(resource);
        if (type == null) {
            type = resource.getClass().getName();
        }

        String idString = null;
        Object id = ObjectUtils.getId(resource);

        if (id != null) {
            idString = id.toString();
        }

        String eventName = getEventName() == null ? process.getName() : getEventName();

        Event request = EventVO.newEvent(eventName).withResourceId(idString).withResourceType(type).withData(state.getData());

        EventCallOptions options = new EventCallOptions(retry, timeoutMillis);
        options.setProgressIsKeepAlive(true);
        options.setProgress(new EventProgress() {
            @Override
            public void progress(Event event) {
                Map<String, Object> data = new HashMap<String, Object>();
                String transitioning = event.getTransitioningMessage();
                Integer progress = event.getTransitioningProgress();

                if (transitioning != null) {
                    data.put(ObjectMetaDataManager.TRANSITIONING_MESSAGE_FIELD, transitioning);
                }

                if (progress != null) {
                    data.put(ObjectMetaDataManager.TRANSITIONING_PROGRESS_FIELD, progress);
                }

                if (data.size() > 0) {
                    Object reloaded = objectManager.reload(resource);
                    objectManager.setFields(reloaded, data);
                }
            }
        });

        try {
            Event response = eventService.callSync(request, options);
            return postEvent(state, process, CollectionUtils.toMap(response.getData()));
        } catch (ExecutionException e) {
            if (!StringUtils.isEmpty(getOnError())) {
                objectProcessManager.scheduleProcessInstance(getOnError(), state.getResource(), state.getData());
                e.setResources(state.getResource());
            }

            throw e;
        }
    }

    protected HandlerResult postEvent(ProcessState state, ProcessInstance process, Map<Object, Object> data) {
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

    @Override
    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public String getOnError() {
        return this.onError;
    }

    public void setOnError(String onError) {
        this.onError = onError;
    }
}