package io.github.ibuildthecloud.dstack.engine.process;

import static io.github.ibuildthecloud.dstack.engine.process.ProcessResult.*;

public enum ExitReason {
    ALREADY_ACTIVE(SUCCESS),
    CANCELED(ProcessResult.CANCELED),
    STATE_CHANGED,
    ACTIVE(SUCCESS),
    FAILED_TO_ACQUIRE_LOCK,
    //TODO rename to listener
    PRE_HANDLER_EXCEPTION,
    PRE_HANDLER_DELAYED,
    HANDLER_EXCEPTION,
    HANDLER_DELAYED,
    POST_HANDLER_EXCEPTION,
    POST_HANDLER_DELAYED,
    UNKNOWN_EXCEPTION;

    boolean terminating;
    ProcessResult result;

    private ExitReason() {
        this(false, null);
    }

    private ExitReason(ProcessResult result) {
        this(true, result);
    }

    private ExitReason(boolean terminating, ProcessResult result) {
        this.terminating = terminating;

        if ( terminating && result == null ) {
            throw new IllegalStateException("All terminating ExitReasons must"
                    + " have a result set");
        }
    }

    public boolean isTerminating() {
        return terminating;
    }

    public ProcessResult getResult() {
        return result;
    }
}
