package io.cattle.platform.lifecycle.util;

/**
 * Indicates that the process has hit a non-recoverable error and action should be
 * taken.
 *
 */
public class LifecycleException extends Exception {

    private static final long serialVersionUID = 1787940590735542060L;

    public LifecycleException() {
        super();
    }

    public LifecycleException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public LifecycleException(String message, Throwable cause) {
        super(message, cause);
    }

    public LifecycleException(String message) {
        super(message);
    }

    public LifecycleException(Throwable cause) {
        super(cause);
    }

}