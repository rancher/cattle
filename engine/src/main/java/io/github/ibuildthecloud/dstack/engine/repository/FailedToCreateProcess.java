package io.github.ibuildthecloud.dstack.engine.repository;

public class FailedToCreateProcess extends RuntimeException {

    private static final long serialVersionUID = -5976618845246932337L;

    public FailedToCreateProcess() {
        super();
    }

    public FailedToCreateProcess(String message, Throwable cause) {
        super(message, cause);
    }

    public FailedToCreateProcess(String message) {
        super(message);
    }

    public FailedToCreateProcess(Throwable cause) {
        super(cause);
    }

}
