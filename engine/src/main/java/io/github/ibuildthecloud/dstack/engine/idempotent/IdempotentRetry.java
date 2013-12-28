package io.github.ibuildthecloud.dstack.engine.idempotent;

public class IdempotentRetry extends RuntimeException {

    private static final long serialVersionUID = 1077770316022177533L;

    public IdempotentRetry() {
        super(createMessage());
    }

    protected static String createMessage() {
        Throwable t = new Throwable();
        t.fillInStackTrace();
        StackTraceElement element = t.getStackTrace()[3];
        return String.format("Idempotent Retry from [%s.%s():%s]",
                element.getClassName(),
                element.getMethodName(),
                element.getLineNumber());
    }
}
