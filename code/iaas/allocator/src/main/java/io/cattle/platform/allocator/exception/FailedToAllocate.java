package io.cattle.platform.allocator.exception;

public class FailedToAllocate extends RuntimeException {

    private static final long serialVersionUID = -6039839043036377124L;

    public FailedToAllocate() {
        super();
    }

    public FailedToAllocate(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public FailedToAllocate(String message, Throwable cause) {
        super(message, cause);
    }

    public FailedToAllocate(String message) {
        super(message);
    }

    public FailedToAllocate(Throwable cause) {
        super(cause);
    }

}
