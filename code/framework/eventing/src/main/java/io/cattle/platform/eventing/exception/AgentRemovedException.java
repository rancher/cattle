package io.cattle.platform.eventing.exception;

import io.cattle.platform.eventing.model.Event;

public class AgentRemovedException extends EventExecutionException {

    private static final long serialVersionUID = 1640368118646924692L;

    public AgentRemovedException(String message, Event event) {
        super(message, event);
    }

}
