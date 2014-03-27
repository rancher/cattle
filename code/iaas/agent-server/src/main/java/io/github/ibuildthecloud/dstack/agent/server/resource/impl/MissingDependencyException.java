package io.github.ibuildthecloud.dstack.agent.server.resource.impl;

public class MissingDependencyException extends Exception {

    private static final long serialVersionUID = 4089321841399691420L;

    public MissingDependencyException() {
        super();
    }

    public MissingDependencyException(String message, Throwable cause) {
        super(message, cause);
    }

    public MissingDependencyException(String message) {
        super(message);
    }

    public MissingDependencyException(Throwable cause) {
        super(cause);
    }

}
