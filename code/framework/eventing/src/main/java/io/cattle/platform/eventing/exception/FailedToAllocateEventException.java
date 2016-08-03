package io.cattle.platform.eventing.exception;

import io.cattle.platform.eventing.model.Event;
import io.cattle.platform.util.exception.LoggableException;

import org.slf4j.Logger;

public class FailedToAllocateEventException extends EventExecutionException implements LoggableException {

    private static final long serialVersionUID = 7682706486276417816L;

    public FailedToAllocateEventException(String message, Event event) {
        super(message, event);
    }

    @Override
    public void log(Logger log) {
        log.info(getMessage());
    }

}
