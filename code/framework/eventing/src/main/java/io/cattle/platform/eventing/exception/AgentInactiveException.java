package io.cattle.platform.eventing.exception;

import io.cattle.platform.eventing.model.Event;

public class AgentInactiveException extends EventExecutionException {

    private static final long serialVersionUID = -8135611084820936474L;

    public AgentInactiveException(String message, Event event) {
        super(message, event);
    }
}
