package io.cattle.platform.util.exception;

import org.slf4j.Logger;

public class ExecutionErrorException extends ExecutionException implements LoggableException {

    private static final long serialVersionUID = -6899106244627174212L;

    public ExecutionErrorException(String message) {
        super(message);
    }

    public ExecutionErrorException(String message, Object resource) {
        super(message, null, resource);
    }

    public ExecutionErrorException(String message, String transitioningMessage, Object... resources) {
        super(message, transitioningMessage, resources);
    }

    public ExecutionErrorException(String message, Throwable cause, String transitioningMessage, Object... resources) {
        super(message, cause, transitioningMessage, resources);
    }

    @Override
    public void log(Logger log) {
        log.error(getMessage());
    }

}
