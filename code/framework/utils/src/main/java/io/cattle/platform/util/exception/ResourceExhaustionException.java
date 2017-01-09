package io.cattle.platform.util.exception;

import org.slf4j.Logger;

public class ResourceExhaustionException extends ExecutionException implements LoggableException {

    private static final long serialVersionUID = 3892324474973389002L;

    public ResourceExhaustionException() {
        super();
    }

    public ResourceExhaustionException(String message, Object resource) {
        super(message, resource);
    }

    public ResourceExhaustionException(String message, String transitioningMessage, Object... resources) {
        super(message, transitioningMessage, resources);
    }

    public ResourceExhaustionException(String message, Throwable cause, String transitioningMessage, Object... resources) {
        super(message, cause, transitioningMessage, resources);
    }

    public ResourceExhaustionException(String message, Throwable cause) {
        super(message, cause);
    }

    public ResourceExhaustionException(String message) {
        super(message);
    }

    public ResourceExhaustionException(Throwable cause) {
        super(cause);
    }

    @Override
    public void log(Logger log) {
        log.info("Failed: {}", getMessage());
    }

}
