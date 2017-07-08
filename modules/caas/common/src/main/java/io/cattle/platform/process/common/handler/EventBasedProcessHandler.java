package io.cattle.platform.process.common.handler;

import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessHandler;
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
import io.cattle.platform.util.type.Priority;
import org.apache.commons.lang.StringUtils;
import org.jooq.exception.DataChangedException;

import java.util.HashMap;
import java.util.Map;

public class EventBasedProcessHandler implements ProcessHandler {

    public static String DEFAULT_NAME = "EventBased";

    EventService eventService;
    String[] processNames;
    String eventName;
    Integer retry;
    Long timeoutMillis;
    String onError;
    int priority = Priority.SPECIFIC;
    ObjectProcessManager processManager;
    ObjectManager objectManager;
    ObjectMetaDataManager metaDataManager;

    public EventBasedProcessHandler(EventService eventService, ObjectManager objectManager, ObjectProcessManager objectProcessManager,
            ObjectMetaDataManager objectMetaDataManager) {
        this();
        this.eventService = eventService;
        this.objectManager = objectManager;
        this.processManager = objectProcessManager;
        this.metaDataManager = objectMetaDataManager;
    }

    public EventBasedProcessHandler() {
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
                Map<String, Object> data = new HashMap<>();
                String transitioning = event.getTransitioningMessage();
                Integer progress = event.getTransitioningProgress();

                if (transitioning != null) {
                    data.put(ObjectMetaDataManager.TRANSITIONING_MESSAGE_FIELD, transitioning);
                }

                if (data.size() > 0) {
                    DataChangedException dce = null;
                    for (int i = 0; i < 3 ; i++) {
                        try {
                            Object reloaded = objectManager.reload(resource);
                            objectManager.setFields(reloaded, data);
                            dce = null;
                            break;
                        } catch (DataChangedException e) {
                            dce = e;
                        }
                    }
                    if (dce != null) {
                        throw dce;
                    }
                }
            }
        });

        try {
            Event response = eventService.callSync(request, options);
            return postEvent(state, process, CollectionUtils.toMap(response.getData()));
        } catch (ExecutionException e) {
            if (!StringUtils.isEmpty(getOnError())) {
                processManager.scheduleProcessInstance(getOnError(), state.getResource(), state.getData());
                e.setResources(state.getResource());
            }

            throw e;
        }
    }

    protected HandlerResult postEvent(ProcessState state, ProcessInstance process, Map<Object, Object> data) {
        return new HandlerResult(data);
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

    public String getOnError() {
        return this.onError;
    }

    public void setOnError(String onError) {
        this.onError = onError;
    }
}