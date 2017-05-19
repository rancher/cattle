package io.cattle.platform.util.exception;

import org.slf4j.Logger;

public class UserException extends ExecutionException implements LoggableException {

    private static final long serialVersionUID = -7085460973157171631L;

    public UserException(String message) {
        super(message);
    }

    public UserException(String message, String transitioningMessage, Object... resources) {
        super(message, transitioningMessage, resources);
    }

    public UserException(String message, Throwable cause, String transitioningMessage, Object... resources) {
        super(message, cause, transitioningMessage, resources);
    }

    @Override
    public void log(Logger log) {
        log.info("User error: [{}]", getMessage());
    }


}
