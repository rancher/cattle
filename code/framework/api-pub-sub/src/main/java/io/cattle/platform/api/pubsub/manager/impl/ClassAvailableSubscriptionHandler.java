package io.cattle.platform.api.pubsub.manager.impl;

import io.cattle.platform.api.pubsub.subscribe.ApiPubSubEventPostProcessor;
import io.cattle.platform.api.pubsub.subscribe.SubscriptionHandler;
import io.cattle.platform.async.retry.RetryTimeoutService;
import io.cattle.platform.eventing.EventService;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.util.type.Priority;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.commons.lang3.reflect.ConstructorUtils;

public class ClassAvailableSubscriptionHandler implements SubscriptionHandler, Priority {

    EventService eventService;
    JsonMapper jsonMapper;
    ExecutorService executorService;
    RetryTimeoutService retryTimeout;
    boolean enabled = false;
    SubscriptionHandler handler;
    String testClass, className;
    int priority = Priority.SPECIFIC;
    List<ApiPubSubEventPostProcessor> eventProcessors;

    @Override
    public boolean subscribe(Collection<String> eventNames, ApiRequest apiRequest, boolean strip) throws IOException {
        if ( enabled ) {
            return handler.subscribe(eventNames, apiRequest, strip);
        } else {
            return false;
        }
    }

    @PostConstruct
    public void init() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        Class<?> clz = getClass(testClass);
        if ( clz != null ) {
            handler = (SubscriptionHandler)ConstructorUtils.invokeConstructor(clz, jsonMapper,
                    eventService, retryTimeout, executorService, eventProcessors);
            enabled = true;
        }
    }

    protected Class<?> getClass(String ifClassName) {
        try {
            Class.forName(ifClassName);
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            return null;
        }
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

    public String getTestClass() {
        return testClass;
    }

    @Inject
    public void setTestClass(String testClass) {
        this.testClass = testClass;
    }

    public String getClassName() {
        return className;
    }

    @Inject
    public void setClassName(String className) {
        this.className = className;
    }

    @Override
    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public List<ApiPubSubEventPostProcessor> getEventProcessors() {
        return eventProcessors;
    }

    @Inject
    public void setEventProcessors(List<ApiPubSubEventPostProcessor> eventProcessors) {
        this.eventProcessors = eventProcessors;
    }

}