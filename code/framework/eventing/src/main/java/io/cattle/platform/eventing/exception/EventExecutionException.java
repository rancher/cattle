package io.cattle.platform.eventing.exception;

import io.cattle.platform.eventing.model.Event;

public class EventExecutionException extends RuntimeException {

    private static final long serialVersionUID = 3499233034118987175L;

    Event event;

    public EventExecutionException(EventExecutionException e) {
        this(e.getEvent());
    }

    public EventExecutionException(Event event) {
        super(event.getTransitioningInternalMessage() == null ?
                event.getTransitioningMessage() : event.getTransitioningInternalMessage());
        this.event = event;
    }

    public Event getEvent() {
        return event;
    }

}
