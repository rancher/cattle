package io.cattle.platform.allocator.exception;

import io.cattle.platform.util.exception.ExecutionException;
import io.cattle.platform.util.exception.LoggableException;

import org.slf4j.Logger;

public class FailedToAllocate extends ExecutionException implements LoggableException {

    private static final long serialVersionUID = -6039839043036377124L;

    public FailedToAllocate() {
        super();
    }

    public FailedToAllocate(String message, Throwable cause) {
        super("Allocation failed: " + message, cause);
    }

    public FailedToAllocate(String message) {
        super("Allocation failed: " + message);
    }

    public FailedToAllocate(Throwable cause) {
        super(cause);
    }

    @Override
    public void log(Logger log) {
        log.info(getMessage());
    }
}
