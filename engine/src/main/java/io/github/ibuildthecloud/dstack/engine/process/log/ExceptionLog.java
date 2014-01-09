package io.github.ibuildthecloud.dstack.engine.process.log;

import io.github.ibuildthecloud.dstack.engine.idempotent.IdempotentRetryException;
import io.github.ibuildthecloud.dstack.util.exception.ExceptionUtils;

public class ExceptionLog {

    String message;
    String clz;
    String cause;
    String stackTrace;

    public ExceptionLog() {
    }

    public ExceptionLog(Throwable t) {
        this.message = t.getMessage();
        this.clz = t.getClass().getName();
        this.stackTrace = t instanceof IdempotentRetryException ? null : ExceptionUtils.toString(t);
        Throwable cause = t.getCause();
        if ( cause != null ) {
            this.cause = cause.getClass().getName();
        }
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getClz() {
        return clz;
    }

    public void setClz(String clz) {
        this.clz = clz;
    }

    public String getStackTrace() {
        return stackTrace;
    }

    public void setStackTrace(String stackTrace) {
        this.stackTrace = stackTrace;
    }

    public String getCause() {
        return cause;
    }

    public void setCause(String cause) {
        this.cause = cause;
    }

}
