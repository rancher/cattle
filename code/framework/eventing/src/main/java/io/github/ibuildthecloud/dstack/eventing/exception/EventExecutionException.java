package io.github.ibuildthecloud.dstack.eventing.exception;

import io.github.ibuildthecloud.dstack.eventing.model.Event;

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
