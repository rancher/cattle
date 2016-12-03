package io.cattle.platform.allocator.service;

import io.cattle.platform.eventing.model.Event;

public class AllocationRequest {

    Event event;
    long resourceId;

    public AllocationRequest(Event event) {
        super();
        this.event = event;
        this.resourceId = Long.parseLong(event.getResourceId());
    }

    public Event getEvent() {
        return event;
    }

    public long getResourceId() {
        return resourceId;
    }

    public void setEvent(Event event) {
        this.event = event;
    }

    public void setResourceId(long resourceId) {
        this.resourceId = resourceId;
    }

    @Override
    public String toString() {
        return "AllocationRequest [type=instance, resource=" + resourceId + "]";
    }

}
