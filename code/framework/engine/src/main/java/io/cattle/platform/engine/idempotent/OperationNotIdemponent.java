package io.cattle.platform.engine.idempotent;

public class OperationNotIdemponent extends RuntimeException {

    private static final long serialVersionUID = -4118987913931869194L;

    public OperationNotIdemponent() {
        super();
    }

    public OperationNotIdemponent(String message, Throwable cause) {
        super(message, cause);
    }

    public OperationNotIdemponent(String message) {
        super(message);
    }

    public OperationNotIdemponent(Throwable cause) {
        super(cause);
    }

}
