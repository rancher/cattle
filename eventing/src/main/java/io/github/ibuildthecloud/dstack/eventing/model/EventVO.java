package io.github.ibuildthecloud.dstack.eventing.model.impl;

import io.github.ibuildthecloud.dstack.eventing.model.Event;

import java.util.Date;
import java.util.UUID;

public class EventVO implements Event {

    String id = UUID.randomUUID().toString();
    String name;
    String[] previousIds;
    Object data;
    Date time = new Date();
    String publisher;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String[] getPreviousIds() {
        return previousIds;
    }

    public void setPreviousIds(String[] previousIds) {
        this.previousIds = previousIds;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public Date getTime() {
        return time;
    }

    public void setTime(Date time) {
        this.time = time;
    }

    public String getPublisher() {
        return publisher;
    }

    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

}
