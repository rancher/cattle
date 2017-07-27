package io.cattle.platform.process.common.handler;

import com.google.common.util.concurrent.ListenableFuture;
import io.cattle.platform.async.utils.AsyncUtils;
import io.cattle.platform.engine.handler.CompletableLogic;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessHandler;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.eventing.EventCallOptions;
import io.cattle.platform.eventing.EventService;
import io.cattle.platform.eventing.model.Event;
import io.cattle.platform.eventing.model.EventVO;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.util.ObjectUtils;
import io.cattle.platform.util.exception.ExecutionException;
import io.cattle.platform.util.type.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.jooq.exception.DataChangedException;

import java.util.HashMap;
import java.util.Map;

public class ExternalProcessHandler implements ProcessHandler, CompletableLogic {

    EventService eventService;
    String handler;
    Integer retry;
    Long timeoutMillis;
    String onError;
    ObjectProcessManager processManager;
    ObjectManager objectManager;
    ObjectMetaDataManager metaDataManager;

    public ExternalProcessHandler(String handler,
                                  EventService eventService,
                                  ObjectManager objectManager,
                                  ObjectProcessManager objectProcessManager,
                                  ObjectMetaDataManager objectMetaDataManager) {
        this.handler = handler;
        this.eventService = eventService;
        this.objectManager = objectManager;
        this.processManager = objectProcessManager;
        this.metaDataManager = objectMetaDataManager;
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

        Event request = EventVO.newEvent(process.getName() + ";handler=" + handler)
                .withResourceId(idString)
                .withResourceType(type)
                .withData(state.getData());

        EventCallOptions options = new EventCallOptions(retry, timeoutMillis);
        options.setProgressIsKeepAlive(true);
        options.setProgress(event -> {
            Map<String, Object> data = new HashMap<>();
            String transitioning = event.getTransitioningMessage();

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
        });

        return new HandlerResult().withFuture(eventService.call(request, options));
    }

    protected HandlerResult postEvent(ProcessState state, ProcessInstance process, Map<Object, Object> data) {
        return new HandlerResult(data);
    }

    @Override
    public HandlerResult complete(ListenableFuture<?> future, ProcessState state, ProcessInstance process) {
        try {
            Event response = (Event)AsyncUtils.get(future);
            return postEvent(state, process, CollectionUtils.toMap(response.getData()));
        } catch (ExecutionException e) {
            String errorProcess = onError;
            if (StringUtils.isEmpty(errorProcess)) {
                errorProcess = process.getName().split("[.]")[0] + ".error";
            }
            processManager.scheduleProcessInstance(errorProcess, state.getResource(), state.getData());
            e.setResources(state.getResource());
            throw e;
        }
    }
}