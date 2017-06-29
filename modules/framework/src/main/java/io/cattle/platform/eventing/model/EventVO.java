package io.cattle.platform.eventing.model;

import io.cattle.platform.eventing.exception.EventExecutionException;

import java.util.Arrays;
import java.util.Date;
import java.util.Map;

import javax.xml.bind.annotation.XmlTransient;

import org.slf4j.MDC;

public class EventVO<T> implements Event {

    String id, name, replyTo, resourceId, resourceType, publisher, transitioning, transitioningMessage, transitioningInternalMessage;
    String[] previousIds, previousNames;
    T data;
    Date time;
    Long timeoutMillis;
    String listenerKey;
    Integer transitioningProgress;
    Map<String, Object> context;

    @SuppressWarnings("unchecked")
    public EventVO() {
        id = io.cattle.platform.util.resource.UUID.randomUUID().toString();
        time = new Date();
        context = MDC.getMDCAdapter().getCopyOfContextMap();
    }

    public EventVO(Event event) {
        this(event, event.getReplyTo());
    }

    @SuppressWarnings("unchecked")
    public EventVO(Event event, String replyTo) {
        this.replyTo = replyTo;

        this.id = event.getId();
        this.name = event.getName();
        this.previousIds = event.getPreviousIds();
        this.previousNames = event.getPreviousNames();
        this.data = (T) event.getData();
        this.time = event.getTime();
        this.publisher = event.getPublisher();
        this.resourceId = event.getResourceId();
        this.resourceType = event.getResourceType();
        this.transitioning = event.getTransitioning();
        this.transitioningMessage = event.getTransitioningMessage();
        this.transitioningInternalMessage = event.getTransitioningInternalMessage();
        this.transitioningProgress = event.getTransitioningProgress();
        this.context = event.getContext();
        this.timeoutMillis = event.getTimeoutMillis();
    }

    public static EventVO<Object> replyWithException(Event request, Class<? extends EventExecutionException> clz,
                                                     String message) {
        EventVO<Object> reply = reply(request);
        reply.setTransitioning(TRANSITIONING_ERROR);
        reply.setTransitioningInternalMessage("class:" + clz.getCanonicalName());
        reply.setTransitioningMessage(message);

        return reply;
    }

    public static EventVO<Object> reply(Event request) {
        String[] previousIds = request.getPreviousIds();
        if (previousIds != null && previousIds.length > 0) {
            String[] newIds = new String[previousIds.length + 1];
            System.arraycopy(previousIds, 0, newIds, 1, previousIds.length);
            newIds[0] = request.getId();

            previousIds = newIds;
        } else {
            previousIds = new String[] { request.getId() };
        }

        EventVO<Object> event = new EventVO<Object>();
        event.setName(request.getReplyTo());
        event.setPreviousNames(prepend(request.getPreviousNames(), request.getName()));
        event.setPreviousIds(prepend(request.getPreviousIds(), request.getId()));
        event.setResourceId(request.getResourceId());
        event.setResourceType(request.getResourceType());

        return event;
    }

    protected static String[] prepend(String[] array, String value) {
        if (array != null && array.length > 0) {
            String[] newIds = new String[array.length + 1];
            System.arraycopy(array, 0, newIds, 1, array.length);
            newIds[0] = value;

            array = newIds;
        } else {
            array = new String[] { value };
        }

        return array;
    }

    public static <T> EventVO<T> newEvent(String name) {
        return new EventVO<T>(name);
    }

    public EventVO(String name) {
        this();
        setName(name);
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public EventVO<T> withName(String name) {
        this.name = name;
        return this;
    }

    @Override
    public String[] getPreviousIds() {
        return previousIds;
    }

    public void setPreviousIds(String[] previousIds) {
        this.previousIds = previousIds;
    }

    public EventVO<T> withPreviousIds(String[] previousIds) {
        this.previousIds = previousIds;
        return this;
    }

    @Override
    public String[] getPreviousNames() {
        return previousNames;
    }

    public void setPreviousNames(String[] names) {
        this.previousNames = names;
    }

    public EventVO<T> withPreviousNames(String[] names) {
        this.previousNames = names;
        return this;
    }

    @Override
    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public EventVO<T> withData(T data) {
        this.data = data;
        return this;
    }

    @Override
    public Date getTime() {
        return time;
    }

    public void setTime(Date time) {
        this.time = time;
    }

    public EventVO<T> withTime(Date time) {
        this.time = time;
        return this;
    }

    @Override
    public String getPublisher() {
        return publisher;
    }

    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }

    public EventVO<T> withPublisher(String publisher) {
        this.publisher = publisher;
        return this;
    }

    @Override
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public EventVO<T> withId(String id) {
        this.id = id;
        return this;
    }

    @Override
    public String getReplyTo() {
        return replyTo;
    }

    public void setReplyTo(String replyTo) {
        this.replyTo = replyTo;
    }

    public EventVO<T> withReplyTo(String replyTo) {
        this.replyTo = replyTo;
        return this;
    }

    @Override
    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public EventVO<T> withResourceId(String resourceId) {
        this.resourceId = resourceId;
        return this;
    }

    @Override
    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public EventVO<T> withResourceType(String resourceType) {
        this.resourceType = resourceType;
        return this;
    }

    @XmlTransient
    public String getListenerKey() {
        return listenerKey;
    }

    public void setListenerKey(String listenerKey) {
        this.listenerKey = listenerKey;
    }

    public EventVO<T> withListenerKey(String listenerKey) {
        this.listenerKey = listenerKey;
        return this;
    }

    @Override
    public String getTransitioning() {
        return transitioning;
    }

    public void setTransitioning(String transitioning) {
        this.transitioning = transitioning;
    }

    public EventVO<T> withTransitioning(String transitioning) {
        this.transitioning = transitioning;
        return this;
    }

    @Override
    public String getTransitioningMessage() {
        return transitioningMessage;
    }

    public void setTransitioningMessage(String transitioningMessage) {
        this.transitioningMessage = transitioningMessage;
    }

    public EventVO<T> withTransitioningMessage(String transitioningMessage) {
        this.transitioningMessage = transitioningMessage;
        return this;
    }

    @Override
    public Integer getTransitioningProgress() {
        return transitioningProgress;
    }

    public void setTransitioningProgress(Integer transitioningProgress) {
        this.transitioningProgress = transitioningProgress;
    }

    public EventVO<T> withTransitioningProgress(Integer transitioningProgress) {
        this.transitioningProgress = transitioningProgress;
        return this;
    }

    @Override
    public String getTransitioningInternalMessage() {
        return transitioningInternalMessage;
    }

    public void setTransitioningInternalMessage(String transitioningInternalMessage) {
        this.transitioningInternalMessage = transitioningInternalMessage;
    }

    public EventVO<T> withTransitioningInternalMessage(String transitioningInternalMessage) {
        this.transitioningInternalMessage = transitioningInternalMessage;
        return this;
    }

    @Override
    public Map<String, Object> getContext() {
        return context;
    }

    public void setContext(Map<String, Object> context) {
        this.context = context;
    }

    @Override
    public Long getTimeoutMillis() {
        return timeoutMillis;
    }

    public void setTimeoutMillis(Long timeout) {
        this.timeoutMillis = timeout;
    }

    public EventVO<T> withTimeoutMillis(Long timeout) {
        this.timeoutMillis = timeout;
        return this;
    }

    public EventVO<T> withContext(Map<String, Object> context) {
        this.context = context;
        return this;
    }

    @Override
    public String toString() {
        return "EventVO [id=" + id + ", name=" + name + ", previousNames=" + Arrays.toString(previousNames) + ", replyTo=" + replyTo + ", resourceId="
                + resourceId + ", resourceType=" + resourceType + ", publisher=" + publisher + ", transitioning=" + transitioning + ", transitioningMessage="
                + transitioningMessage + ", transitioningInternalMessage=" + transitioningInternalMessage + ", previousIds=" + Arrays.toString(previousIds)
                + ", data=" + data + ", time=" + time + ", listenerKey=" + listenerKey + ", transitioningProgress=" + transitioningProgress + "]";
    }

}
