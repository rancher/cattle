package io.cattle.platform.eventing.model;

import javax.xml.bind.annotation.XmlTransient;

public class EventVO<T, K> implements Event {

    private String id, name, replyTo, resourceId, resourceType, transitioning, transitioningMessage;
    private String previousId;
    private T data;
    private K requestData;
    private Long time;
    private Long timeoutMillis;
    private String listenerKey;

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
        this.data = (T) event.getData();
        this.name = event.getName();
        this.previousId = event.getPreviousId();
        this.resourceId = event.getResourceId();
        this.resourceType = event.getResourceType();
        this.time = event.getTime();
        this.timeoutMillis = event.getTimeoutMillis();
        this.transitioning = event.getTransitioning();
        this.transitioningMessage = event.getTransitioningMessage();
    }

    public static EventVO<Object, Object> reply(Event request) {
        EventVO<Object, Object> event = new EventVO<>();
        event.setName(request.getReplyTo());
        event.setPreviousId(request.getId());
        return event;
    }

    public static <T, K> EventVO<T, K> newEvent(String name) {
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

    public EventVO<T, K> withName(String name) {
        this.name = name;
        return this;
    }

    @Override
    public String getPreviousId() {
        return previousId;
    }

    public void setPreviousId(String previousId) {
        this.previousId = previousId;
    }

    public EventVO<T, K> withPreviousId(String previousId) {
        this.previousId = previousId;
        return this;
    }

    @Override
    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public EventVO<T, K> withData(T data) {
        this.data = data;
        return this;
    }

    @Override
    public K getRequestData() {
        return requestData;
    }

    public void setRequestData(K requestData) {
        this.requestData = requestData;
    }

    public EventVO<T, K> withRequestData(K requestData) {
        this.requestData = requestData;
        return this;
    }

    @Override
    public Long getTime() {
        return time;
    }

    public void setTime(Long time) {
        this.time = time;
    }

    public EventVO<T, K> withTime(Long time) {
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

    public EventVO<T, K> withId(String id) {
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

    public EventVO<T, K> withReplyTo(String replyTo) {
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

    public EventVO<T, K> withResourceId(String resourceId) {
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

    public EventVO<T, K> withResourceType(String resourceType) {
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

    public EventVO<T, K> withListenerKey(String listenerKey) {
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

    public EventVO<T, K> withTransitioning(String transitioning) {
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

    public EventVO<T, K> withTransitioningMessage(String transitioningMessage) {
        this.transitioningMessage = transitioningMessage;
        return this;
    }

    @Override
    public Long getTimeoutMillis() {
        return timeoutMillis;
    }

    public void setTimeoutMillis(Long timeout) {
        this.timeoutMillis = timeout;
    }

    public EventVO<T, K> withTimeoutMillis(Long timeout) {
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
                ", previousId='" + previousId + '\'' +
                ", data=" + data +
                ", requestData=" + requestData +
                ", time=" + time +
                ", timeoutMillis=" + timeoutMillis +
                ", listenerKey='" + listenerKey + '\'' +
                '}';
    }

}
