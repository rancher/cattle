package io.github.ibuildthecloud.dstack.eventing.model;

import io.github.ibuildthecloud.dstack.eventing.model.Event;

import java.util.Arrays;
import java.util.Date;
import java.util.UUID;

import javax.xml.bind.annotation.XmlTransient;

public class EventVO implements Event {

    String id, name, replyTo, resourceId, resourceType, publisher;
    String[] previousIds;
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
        this.data = event.getData();
        this.time = event.getTime();
        this.publisher = event.getPublisher();
        this.resourceId = event.getResourceId();
        this.resourceType = event.getResourceType();
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String[] getPreviousIds() {
        return previousIds;
    }

    public void setPreviousIds(String[] previousIds) {
        this.previousIds = previousIds;
    }

    @Override
    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    @Override
    public Date getTime() {
        return time;
    }

    public void setTime(Date time) {
        this.time = time;
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

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String getReplyTo() {
        return replyTo;
    }

    public void setReplyTo(String replyTo) {
        this.replyTo = replyTo;
    }

    @Override
    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    @Override
    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    @Override
    public String toString() {
        return "EventVO [id=" + id + ", name=" + name + ", replyTo=" + replyTo + ", resourceId=" + resourceId
                + ", resourceType=" + resourceType + ", publisher=" + publisher + ", previousIds="
                + Arrays.toString(previousIds) + ", data=" + data + ", time=" + time + "]";
    }

    @XmlTransient
    public String getListenerKey() {
        return listenerKey;
    }

    public void setListenerKey(String listenerKey) {
        this.listenerKey = listenerKey;
    }

}
