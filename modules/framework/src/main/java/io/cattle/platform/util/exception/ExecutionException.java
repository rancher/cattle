package io.cattle.platform.util.exception;

public class ExecutionException extends RuntimeException {

    private static final long serialVersionUID = -6264703257346922100L;

    Object[] resources = new Object[0];

    public ExecutionException() {
    }

    public ExecutionException(String message) {
        super(message);
    }

    public ExecutionException(String message, Throwable cause) {
        super(message, cause);
    }

    public ExecutionException(String message, Throwable cause, Object... resources) {
        super(message, cause);
        this.resources = resources;
    }

    public ExecutionException(String message, Object... resources) {
        super(message);
        this.resources = resources;
    }

    public ExecutionException(Throwable cause, Object... resources) {
        this(cause.getMessage(), cause, resources);
    }

    public String getTransitioningMessage() {
        return getMessage();
    }

    public Object[] getResources() {
        return resources;
    }

    public void setResources(Object... resources) {
        this.resources = resources;
    }

}