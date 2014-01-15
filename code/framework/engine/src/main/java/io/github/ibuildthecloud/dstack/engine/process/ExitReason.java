package io.github.ibuildthecloud.dstack.engine.process;

import static io.github.ibuildthecloud.dstack.engine.process.ProcessResult.*;

public enum ExitReason {
    ALREADY_ACTIVE(SUCCESS),
    CANCELED(ProcessResult.CANCELED),
    STATE_CHANGED,
    ACTIVE(SUCCESS),
    DELEGATE,
    SERVER_TERMINATED,
    SCHEDULED,
    FAILED_TO_ACQUIRE_PROCESS_INSTANCE_LOCK,
    FAILED_TO_ACQUIRE_LOCK,
    PRE_LISTENER_EXCEPTION,
    PRE_LISTENER_DELAYED,
    HANDLER_EXCEPTION,
    HANDLER_DELAYED,
    POST_LISTENER_EXCEPTION,
    POST_LISTENER_DELAYED,
    RETRY_EXCEPTION(true),
    UNKNOWN_EXCEPTION(true),
    MISSING_HANDLER_RESULT_FIELDS;

    boolean terminating;
    boolean rethrow;
    ProcessResult result;

    private ExitReason() {
        this(false, null);
    }

    private ExitReason(boolean rethrow) {
        this(false, null);
        this.rethrow = rethrow;
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

    public boolean isRethrow() {
        return rethrow;
    }
}
