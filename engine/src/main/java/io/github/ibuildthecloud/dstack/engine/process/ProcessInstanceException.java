package io.github.ibuildthecloud.dstack.engine.process;

import io.github.ibuildthecloud.dstack.engine.process.impl.ProcessExecutionExitException;

public class ProcessInstanceException extends RuntimeException {

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

}
