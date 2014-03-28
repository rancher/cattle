package io.cattle.platform.engine.manager;

public class ProcessNotFoundException extends RuntimeException {

    private static final long serialVersionUID = -5976618845246932337L;

    public ProcessNotFoundException() {
        super();
    }

    public ProcessNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public ProcessNotFoundException(String message) {
        super(message);
    }

    public ProcessNotFoundException(Throwable cause) {
        super(cause);
    }

}
