package io.cattle.platform.engine.process;

import static io.cattle.platform.engine.process.ProcessResult.*;

public enum ExitReason {
    ALREADY_DONE(SUCCESS),
    CANCELED(ProcessResult.CANCELED),
    STATE_CHANGED,
    DONE(SUCCESS),
    DELEGATE,
    SERVER_TERMINATED,
    SCHEDULED,
    PROCESS_ALREADY_IN_PROGRESS,
    RESOURCE_BUSY,
    RETRY_EXCEPTION(true),
    UNKNOWN_EXCEPTION(true),
    TIMEOUT(true),
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
