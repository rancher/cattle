package io.cattle.platform.util.exception;

import java.io.PrintWriter;
import java.io.StringWriter;

public class ExceptionUtils {
    @SuppressWarnings("unchecked")
    public static <T extends Throwable> void rethrow(Throwable t, Class<T> clz) throws T {
        if (clz.isAssignableFrom(t.getClass()))
            throw (T) t;
    }

    public static <T extends Throwable> void rethrowRuntime(Throwable t) {
        rethrow(t, RuntimeException.class);
        rethrow(t, Error.class);
    }

    public static <T extends Throwable> void throwRuntime(String message, Throwable t) {
        rethrow(t, RuntimeException.class);
        rethrow(t, Error.class);

        throw new RuntimeException(message, t);
    }

    public static <T extends Throwable> void rethrowExpectedRuntime(Throwable t) {
        rethrowRuntime(t);
        throw new IllegalStateException(t);
    }

    public static String toString(Throwable t) {
        StringWriter writer = new StringWriter();
        PrintWriter printWriter = new PrintWriter(writer);
        t.printStackTrace(printWriter);
        printWriter.close();
        return writer.toString();
    }

}
