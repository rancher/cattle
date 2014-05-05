package io.cattle.platform.util.exception;

public class ExecutionException extends RuntimeException {

    private static final long serialVersionUID = -6264703257346922100L;

    String transitioningMessage;
    Object[] resources = new Object[0];

    public ExecutionException() {
        super();
    }

    public ExecutionException(String message, Throwable cause) {
        super(message, cause);
    }

    public ExecutionException(String message, Throwable cause, String transitioningMessage, Object... resources) {
        super(message, cause);
        this.transitioningMessage = transitioningMessage;
        this.resources = resources;
    }

    public ExecutionException(String message) {
        super(message);
    }

    public ExecutionException(String message, String transitioningMessage, Object... resources) {
        super(message);
        this.transitioningMessage = transitioningMessage;
        this.resources = resources;
    }

    public ExecutionException(String message, Object resource) {
        this(message, null, resource);
    }

    public ExecutionException(Throwable cause) {
        super(cause);
    }

    public String getTransitioningMessage() {
        return transitioningMessage;
    }

    public String getTransitioningInternalMessage() {
        return getMessage();
    }

    public Object[] getResources() {
        return resources;
    }

    public void setResources(Object... resources) {
        this.resources = resources;
    }

}