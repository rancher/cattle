package io.cattle.platform.engine.process.impl;

import io.cattle.platform.engine.process.ExitReason;
import io.cattle.platform.engine.process.ProcessResult;
import io.cattle.platform.util.exception.LoggableException;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;

public class ProcessExecutionExitException extends RuntimeException implements LoggableException {
    private static final long serialVersionUID = 1L;

    ExitReason exitReason;

    public ProcessExecutionExitException(ExitReason exitReason) {
        this(exitReason, null);
    }

    public ProcessExecutionExitException(String message, ExitReason exitReason) {
        this(message, exitReason, null);
    }

    public ProcessExecutionExitException(ExitReason exitReason, Throwable t) {
        this(null, exitReason, t);
    }

    public ProcessExecutionExitException(String message, ExitReason exitReason, Throwable t) {
        super(message == null ? exitReason.toString() : message, t);
        this.exitReason = exitReason;
    }

    public ExitReason getExitReason() {
        return exitReason;
    }

    @Override
    public void log(Logger log) {
        if (this.getExitReason() != null && this.getExitReason().getResult() == ProcessResult.SUCCESS) {
            return;
        } else if (this.getExitReason().isRethrow()) {
            if (this.getExitReason().isError()) {
                log.error("Exiting with code [{}] : {} : [{}]", this.getExitReason(), this.getCause().getClass().getSimpleName(), this.getCause().getMessage());
            } else if (this.getCause() == null) {
                log.debug("Exiting with code [{}] : {} : [{}]", this.getExitReason(), this.getClass().getSimpleName(), this.getMessage());
            } else {
                log.debug("Exiting with code [{}] : {} : [{}]", this.getExitReason(), this.getCause().getClass().getSimpleName(), this.getCause().getMessage());
            }
        } else if (this.getExitReason().isError()) {
            log.error("Exiting with code [{}] : {}", this.getExitReason(), this.getMessage(), this.getCause());
        } else {
            log.debug("Exiting with code [{}] : {}", this.getExitReason(), this.getMessage(),
                    this.getCause() != null ? this.getCause().getMessage() : StringUtils.EMPTY);
        }
    }

}
