package io.github.ibuildthecloud.dstack.engine.process;

public class ProcessExecutionExitException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    ExitReason exitReason;

    public ProcessExecutionExitException(ExitReason exitReason) {
        this(null, exitReason);
    }

    public ProcessExecutionExitException(String message, ExitReason exitReason) {
        super(message);
        this.exitReason = exitReason;
    }

    public ExitReason getExitReason() {
        return exitReason;
    }

}
