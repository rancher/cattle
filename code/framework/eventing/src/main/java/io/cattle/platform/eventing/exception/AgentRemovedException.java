package io.cattle.platform.eventing.exception;

import io.cattle.platform.eventing.model.Event;

public class AgentRemovedException extends EventExecutionException {

    public AgentRemovedException(String message, Event event) {
        super(message, event);
    }

}
