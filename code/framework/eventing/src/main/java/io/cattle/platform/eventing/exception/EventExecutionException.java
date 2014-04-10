package io.cattle.platform.eventing.exception;

import io.cattle.platform.eventing.model.Event;
import io.cattle.platform.util.exception.ExecutionException;

public class EventExecutionException extends ExecutionException {

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

    @Override
    public String getTransitioningMessage() {
        return event == null ? super.getTransitioningMessage() : event.getTransitioningMessage();
    }

    @Override
    public String getTransitioningInternalMessage() {
        return event == null ? super.getTransitioningInternalMessage() : event.getTransitioningInternalMessage();
    }

}
