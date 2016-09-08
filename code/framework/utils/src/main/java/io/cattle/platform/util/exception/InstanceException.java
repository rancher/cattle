package io.cattle.platform.util.exception;

import org.slf4j.Logger;

public class InstanceException extends IllegalStateException implements LoggableException {
    private static final long serialVersionUID = 4868400759427367403L;

    Object instance;

    public InstanceException() {
        super();
    }

    public InstanceException(String message, Throwable cause, Object instance) {
        super(message + ": " + cause.getMessage());
        this.instance = instance;
    }

    public InstanceException(String message, Object instance) {
        super(message);
        this.instance = instance;
    }

    public Object getInstance() {
        return instance;
    }

    @Override
    public void log(Logger log) {
        log.error(this.getMessage());
    }
}
