package io.cattle.platform.engine.process;

import io.cattle.platform.engine.process.impl.ProcessExecutionExitException;
import io.cattle.platform.util.exception.LoggableException;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;

public class ProcessInstanceException extends RuntimeException implements LoggableException {

    private static final long serialVersionUID = -6265541865661635324L;

    ProcessInstance processInstance;
    ExitReason exitReason;

    public ProcessInstanceException(ProcessInstance processInstance, ProcessExecutionExitException t) {
        super(t);
        this.processInstance = processInstance;
        this.exitReason = t.getExitReason();
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
                log.error("Exiting with code [{}] : {} : [{}]", this.getExitReason(), this.getCause().getClass()
                        .getSimpleName(), this.getCause().getMessage());
            } else {
                log.debug("Exiting with code [{}] : {} : [{}]", this.getExitReason(), this.getCause().getClass()
                        .getSimpleName(), this.getCause().getMessage());
            }
        } else if (this.getExitReason().isError()) {
            log.error("Exiting with code [{}] : {}", this.getExitReason(), this.getMessage(), this.getCause());
        } else {
            log.debug("Exiting with code [{}] : {}", this.getExitReason(), this.getMessage(),
                    this.getCause() != null ? this.getCause().getMessage() : StringUtils.EMPTY);
        }
    }

}
