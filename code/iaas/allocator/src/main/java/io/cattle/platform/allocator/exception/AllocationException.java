package io.cattle.platform.allocator.exception;

public class AllocationException extends RuntimeException {

    private static final long serialVersionUID = -6039839043036377124L;

    public AllocationException() {
        super();
    }

    public AllocationException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public AllocationException(String message, Throwable cause) {
        super(message, cause);
    }

    public AllocationException(String message) {
        super(message);
    }

    public AllocationException(Throwable cause) {
        super(cause);
    }

}
