package io.cattle.platform.eventing.model;

import javax.xml.bind.annotation.XmlTransient;
import java.util.Arrays;

public class EventVO<T> implements Event {

    private String id, name, replyTo, resourceId, resourceType, transitioning, transitioningMessage;
    private String[] previousIds, previousNames;
    private T data;
    private Long time;
    private Long timeoutMillis;
    private String listenerKey;
    private Integer transitioningProgress;

    @SuppressWarnings("unchecked")
    public EventVO() {
        id = io.cattle.platform.util.resource.UUID.randomUUID().toString();
        time = System.currentTimeMillis();
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
        this.resourceId = event.getResourceId();
        this.resourceType = event.getResourceType();
        this.transitioning = event.getTransitioning();
        this.transitioningMessage = event.getTransitioningMessage();
        this.transitioningProgress = event.getTransitioningProgress();
        this.timeoutMillis = event.getTimeoutMillis();
    }

    public static EventVO<Object> reply(Event request) {
        EventVO<Object> event = new EventVO<>();
        event.setName(request.getReplyTo());
        event.setPreviousNames(prepend(request.getPreviousNames(), request.getName()));
        event.setPreviousIds(prepend(request.getPreviousIds(), request.getId()));
        event.setResourceId(request.getResourceId());
        event.setResourceType(request.getResourceType());

        return event;
    }

    private static String[] prepend(String[] array, String value) {
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
        return new EventVO<>(name);
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
    public Long getTime() {
        return time;
    }

    public void setTime(Long time) {
        this.time = time;
    }

    public EventVO<T> withTime(Long time) {
        this.time = time;
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

    @Override
    public String toString() {
        return "EventVO{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", replyTo='" + replyTo + '\'' +
                ", resourceId='" + resourceId + '\'' +
                ", resourceType='" + resourceType + '\'' +
                ", transitioning='" + transitioning + '\'' +
                ", transitioningMessage='" + transitioningMessage + '\'' +
                ", previousIds=" + Arrays.toString(previousIds) +
                ", previousNames=" + Arrays.toString(previousNames) +
                ", data=" + data +
                ", time=" + time +
                ", timeoutMillis=" + timeoutMillis +
                ", listenerKey='" + listenerKey + '\'' +
                ", transitioningProgress=" + transitioningProgress +
                '}';
    }

}
