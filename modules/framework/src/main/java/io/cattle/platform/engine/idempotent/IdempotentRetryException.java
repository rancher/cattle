package io.cattle.platform.engine.idempotent;

public class IdempotentRetryException extends RuntimeException {

    private static final long serialVersionUID = 1077770316022177533L;

    public IdempotentRetryException() {
        super(createMessage());
    }

    protected static String createMessage() {
        Throwable t = new Throwable();
        t.fillInStackTrace();
        StackTraceElement element = t.getStackTrace()[3];
        return String.format("Idempotent Retry from [%s.%s():%s]", element.getClassName(), element.getMethodName(), element.getLineNumber());
    }
}
