package io.github.ibuildthecloud.dstack.eventing.model;

import io.github.ibuildthecloud.dstack.eventing.model.Event;

import java.util.Arrays;
import java.util.Date;
import java.util.UUID;

import javax.xml.bind.annotation.XmlTransient;

public class EventVO implements Event {

    String id, name, replyTo, resourceId, resourceType, publisher;
    String[] previousIds, previousNames;
    Object data;
    Date time;
    String listenerKey;

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

    public EventVO name(String name) {
        setName(name);
        return this;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public EventVO previousIds(String[] previousIds) {
        setPreviousIds(previousIds);
        return this;
    }

    @Override
    public String[] getPreviousIds() {
        return previousIds;
    }

    public void setPreviousIds(String[] previousIds) {
        this.previousIds = previousIds;
    }

    @Override
    public String[] getPreviousNames() {
        return previousNames;
    }

    public void setPreviousNames(String[] names) {
        this.previousNames = names;
    }

    @Override
    public Object getData() {
        return data;
    }

    public EventVO data(Object data) {
        this.setData(data);
        return this;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public EventVO time(Date time) {
        setTime(time);
        return this;
    }

    @Override
    public Date getTime() {
        return time;
    }

    public void setTime(Date time) {
        this.time = time;
    }

    public EventVO publisher(String publisher) {
        setPublisher(publisher);
        return this;
    }

    @Override
    public String getPublisher() {
        return publisher;
    }

    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }

    @Override
    public String getId() {
        return id;
    }

    public EventVO id(String id) {
        setId(id);
        return this;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String getReplyTo() {
        return replyTo;
    }

    public EventVO replyTo(String replyTo) {
        setReplyTo(replyTo);
        return this;
    }

    public void setReplyTo(String replyTo) {
        this.replyTo = replyTo;
    }

    @Override
    public String getResourceId() {
        return resourceId;
    }

    public EventVO resourceId(String resourceId) {
        setResourceId(resourceId);
        return this;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    @Override
    public String getResourceType() {
        return resourceType;
    }

    public EventVO resourceType(String resourceType) {
        setResourceType(resourceType);
        return this;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    @Override
    public String toString() {
        return "EventVO [id=" + id + ", name=" + name + ", previousNames=" + Arrays.toString(previousNames)
                + ", replyTo=" + replyTo + ", resourceId=" + resourceId + ", resourceType=" + resourceType
                + ", publisher=" + publisher + ", previousIds=" + Arrays.toString(previousIds) + ", data=" + data
                + ", time=" + time + ", listenerKey=" + listenerKey + "]";
    }

    @XmlTransient
    public String getListenerKey() {
        return listenerKey;
    }

    public EventVO listenerKey(String listenerKey) {
        setListenerKey(listenerKey);
        return this;
    }

    public void setListenerKey(String listenerKey) {
        this.listenerKey = listenerKey;
    }

}
