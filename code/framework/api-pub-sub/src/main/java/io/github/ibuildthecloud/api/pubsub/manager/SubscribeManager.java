package io.github.ibuildthecloud.api.pubsub.manager;

import io.github.ibuildthecloud.api.pubsub.model.Subscribe;
import io.github.ibuildthecloud.api.pubsub.subscribe.BlockingSubscriptionHandler;
import io.github.ibuildthecloud.api.pubsub.subscribe.SubscriptionHandler;
import io.github.ibuildthecloud.api.pubsub.util.SubscriptionUtils;
import io.github.ibuildthecloud.api.pubsub.util.SubscriptionUtils.SubscriptionStyle;
import io.github.ibuildthecloud.dstack.api.auth.Policy;
import io.github.ibuildthecloud.dstack.api.utils.ApiUtils;
import io.github.ibuildthecloud.dstack.async.retry.RetryTimeoutService;
import io.github.ibuildthecloud.dstack.eventing.EventService;
import io.github.ibuildthecloud.dstack.framework.event.FrameworkEvents;
import io.github.ibuildthecloud.dstack.json.JsonMapper;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.exception.ValidationErrorException;
import io.github.ibuildthecloud.gdapi.model.ListOptions;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManager;
import io.github.ibuildthecloud.gdapi.request.resource.impl.AbstractNoOpResourceManager;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;
import io.github.ibuildthecloud.gdapi.validation.ValidationErrorCodes;
import io.github.ibuildthecloud.model.Pagination;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.commons.lang3.reflect.ConstructorUtils;
import org.osgi.framework.FrameworkEvent;

public class SubscribeManager extends AbstractNoOpResourceManager {

    List<SubscriptionHandler> handlers = new ArrayList<SubscriptionHandler>();
    EventService eventService;
    JsonMapper jsonMapper;
    ExecutorService executorService;
    RetryTimeoutService retryTimeout;

    @Override
    public Class<?>[] getTypeClasses() {
        return new Class<?>[] { Subscribe.class };
    }

    @Override
    protected Object createInternal(String type, ApiRequest request) {
        Subscribe subscribe = request.proxyRequestObject(Subscribe.class);

        List<String> eventNames = subscribe.getEventNames();
        List<String> filteredEventNames = new ArrayList<String>(eventNames.size());

        Policy policy = ApiUtils.getPolicy();

        SubscriptionStyle style = SubscriptionUtils.getSubscriptionStyle(policy);
        for ( String eventName : eventNames ) {
            switch (style) {
            case QUALIFIED:
                String key = SubscriptionUtils.getSubscriptionQualifier(policy);
                String value = SubscriptionUtils.getSubscriptionQualifierValue(policy);
                eventName = String.format("%s%s%s=%s", eventName, FrameworkEvents.EVENT_SEP, key, value);
                break;
            case RAW:
                break;
            }

            filteredEventNames.add(eventName);
        }

        request.setResponseContentType("text/plain");

        try {
            for ( SubscriptionHandler handler : handlers ) {
                if ( handler.subscribe(filteredEventNames, request, style != SubscriptionStyle.RAW) ) {
                    return new Object();
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to subscribe to [" + filteredEventNames + "]", e);
        }

        return super.createInternal(type, request);
    }


    @PostConstruct
    public void init() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        Class<?> clz = getClass("javax.servlet.AsyncContext");
        if ( clz != null ) {
            SubscriptionHandler handler = (SubscriptionHandler)ConstructorUtils.invokeConstructor(clz, jsonMapper,
                    eventService, retryTimeout, executorService);
            handlers.add(handler);
        }

        handlers.add(new BlockingSubscriptionHandler(jsonMapper, eventService, retryTimeout, executorService));
    }

    protected Class<?> getClass(String ifClassName) {
        try {
            Class.forName(ifClassName);
            return Class.forName("io.github.ibuildthecloud.api.pubsub.subscribe.ServletAsyncSubscriptionHandler");
        } catch (ClassNotFoundException e) {
            return null;
        }
    }
    protected Long getAgent(Long agentId) {
        if ( agentId != null ) {
            return agentId;
        }

        String type = getLocator().getType(Agent.class);
        ResourceManager rm = getLocator().getResourceManagerByType(type);
        List<?> agents = rm.list(type, null,Pagination.limit(2));

        if ( agents.size() > 1 ) {
            throw new ValidationErrorException(ValidationErrorCodes.MISSING_REQUIRED, "agentId");
        }

        return agents.size() == 0 ? null : ((Agent)agents.get(1)).getId();
    }

    @Override
    protected Object listInternal(String type, Map<Object, Object> criteria, ListOptions options) {
        return Collections.EMPTY_LIST;
    }

    public EventService getEventService() {
        return eventService;
    }

    @Inject
    public void setEventService(EventService eventService) {
        this.eventService = eventService;
    }

    public JsonMapper getJsonMapper() {
        return jsonMapper;
    }

    @Inject
    public void setJsonMapper(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    public ExecutorService getExecutorService() {
        return executorService;
    }

    @Inject
    public void setExecutorService(ExecutorService executorService) {
        this.executorService = executorService;
    }

    public RetryTimeoutService getRetryTimeout() {
        return retryTimeout;
    }

    @Inject
    public void setRetryTimeout(RetryTimeoutService retryTimeout) {
        this.retryTimeout = retryTimeout;
    }

}
