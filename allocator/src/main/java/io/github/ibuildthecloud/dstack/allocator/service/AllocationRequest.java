package io.github.ibuildthecloud.dstack.allocator.service;

import io.github.ibuildthecloud.dstack.eventing.model.Event;

public class AllocationRequest {

    public enum Type {
        INSTANCE;
    }

    Type type;
    Event event;
    long resourceId;
    boolean sendReply = true;

    public AllocationRequest(Event event) {
        super();
        this.event = event;
        if ( event.getName().startsWith("instance") ) {
            this.type = Type.INSTANCE;
        }
        this.resourceId = Long.parseLong(event.getResourceId());
    }

    public Type getType() {
        return type;
    }

    public Event getEvent() {
        return event;
    }

    public long getResourceId() {
        return resourceId;
    }

    public boolean isSendReply() {
        return sendReply;
    }

    public void setSendReply(boolean sendReply) {
        this.sendReply = sendReply;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public void setEvent(Event event) {
        this.event = event;
    }

    public void setResourceId(long resourceId) {
        this.resourceId = resourceId;
    }

    @Override
    public String toString() {
        return "AllocationRequest [type=" + type + ", resource=" + resourceId + "]";
    }

}
