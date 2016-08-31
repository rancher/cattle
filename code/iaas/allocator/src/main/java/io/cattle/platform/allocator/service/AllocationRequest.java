package io.cattle.platform.allocator.service;

import io.cattle.platform.eventing.model.Event;

public class AllocationRequest {

    public enum Type {
        INSTANCE, VOLUME;
    }

    Type type;
    Event event;
    long resourceId;

    public AllocationRequest(Event event) {
        super();
        this.event = event;
        if (event.getName().startsWith("instance")) {
            this.type = Type.INSTANCE;
        } else if (event.getName().startsWith("volume")) {
            this.type = Type.VOLUME;
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
