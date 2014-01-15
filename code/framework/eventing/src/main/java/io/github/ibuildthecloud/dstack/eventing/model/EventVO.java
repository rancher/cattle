package io.github.ibuildthecloud.dstack.eventing.model;

import io.github.ibuildthecloud.dstack.eventing.model.Event;

import java.util.Arrays;
import java.util.Date;
import java.util.UUID;

import javax.xml.bind.annotation.XmlTransient;

public class EventVO implements Event {

    String id, name, replyTo, resourceId, resourceType, publisher, transitioning, transitioningMessage, transitioningInternalMessage;
    String[] previousIds, previousNames;
    Object data;
    Date time;
    String listenerKey;
    Integer transitioningProgress;

    public EventVO() {
        id = UUID.randomUUID().toString();
        time = new Date();
    }

    public EventVO(Event event) {
        this(event, event.getReplyTo());
    }

    public EventVO(Event event, String replyTo) {
        this.replyTo = replyTo;

        this.id = event.getId();
        this.name = event.getName();
        this.previousIds = event.getPreviousIds();
        this.previousNames = event.getPreviousNames();
        this.data = event.getData();
        this.time = event.getTime();
        this.publisher = event.getPublisher();
        this.resourceId = event.getResourceId();
        this.resourceType = event.getResourceType();
        this.transitioning = event.getTransitioning();
        this.transitioningMessage = event.getTransitioningMessage();
        this.transitioningInternalMessage = event.getTransitioningInternalMessage();
        this.transitioningProgress = event.getTransitioningProgress();
    }

    public static EventVO reply(Event request) {
        String[] previousIds = request.getPreviousIds();
        if ( previousIds != null && previousIds.length > 0 ) {
            String[] newIds = new String[previousIds.length+1];
            System.arraycopy(previousIds, 0, newIds, 1, previousIds.length);
            newIds[0] = request.getId();

            previousIds = newIds;
        } else {
            previousIds = new String[] { request.getId() };
        }

        EventVO event = new EventVO();
        event.setName(request.getReplyTo());
        event.setPreviousNames(prepend(request.getPreviousNames(), request.getName()));
        event.setPreviousIds(prepend(request.getPreviousIds(), request.getId()));
        event.setResourceId(request.getResourceId());
        event.setResourceType(request.getResourceType());

        return event;
    }

    protected static String[] prepend(String[] array, String value) {
        if ( array != null && array.length > 0 ) {
            String[] newIds = new String[array.length+1];
            System.arraycopy(array, 0, newIds, 1, array.length);
            newIds[0] = value;

            array = newIds;
        } else {
            array = new String[] { value };
        }

        return array;
    }

    public static EventVO newEvent(String name) {
        return new EventVO(name);
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

    public EventVO withName(String name) {
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

    public EventVO withPreviousIds(String[] previousIds) {
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

    public EventVO withPreviousNames(String[] names) {
        this.previousNames = names;
        return this;
    }

    @Override
    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public EventVO withData(Object data) {
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

    public EventVO withTime(Date time) {
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

    public EventVO withPublisher(String publisher) {
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

    public EventVO withId(String id) {
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

    public EventVO withReplyTo(String replyTo) {
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

    public EventVO withResourceId(String resourceId) {
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

    public EventVO withResourceType(String resourceType) {
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

    public EventVO withListenerKey(String listenerKey) {
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

    public EventVO withTransitioning(String transitioning) {
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

    public EventVO withTransitioningMessage(String transitioningMessage) {
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

    public EventVO withTransitioningProgress(Integer transitioningProgress) {
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

    public EventVO withTransitioningInternalMessage(String transitioningInternalMessage) {
        this.transitioningInternalMessage = transitioningInternalMessage;
        return this;
    }

    @Override
    public String toString() {
        return "EventVO [id=" + id + ", name=" + name + ", previousNames=" + Arrays.toString(previousNames)
                + ", replyTo=" + replyTo + ", resourceId=" + resourceId + ", resourceType=" + resourceType
                + ", publisher=" + publisher + ", transitioning=" + transitioning + ", transitioningMessage="
                + transitioningMessage + ", transitioningInternalMessage=" + transitioningInternalMessage
                + ", previousIds=" + Arrays.toString(previousIds) + ", data=" + data + ", time=" + time
                + ", listenerKey=" + listenerKey + ", transitioningProgress=" + transitioningProgress + "]";
    }

}
