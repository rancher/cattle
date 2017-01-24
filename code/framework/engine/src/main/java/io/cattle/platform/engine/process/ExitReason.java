package io.cattle.platform.engine.process;

import static io.cattle.platform.engine.process.ProcessResult.*;

public enum ExitReason {
    ALREADY_DONE(SUCCESS),
    CANCELED(ProcessResult.CANCELED, true),
    DELAY(true, false, false, null),
    STATE_CHANGED(false, false, false, null),
    DONE(SUCCESS),
    DELEGATE(false, false, false, null),
    SCHEDULED(false, false, false, null),
    PROCESS_ALREADY_IN_PROGRESS(false, false, false, null),
    RESOURCE_BUSY(false, false, false, null),
    RETRY_EXCEPTION(true),
    UNKNOWN_EXCEPTION(true),
    TIMEOUT(true),
    CHAIN(SUCCESS);

    boolean terminating;
    boolean rethrow;
    boolean error;
    ProcessResult result;

    private ExitReason() {
        this(false, false, true, null);
    }

    private ExitReason(boolean rethrow) {
        this(rethrow, false, true, null);
    }

    private ExitReason(ProcessResult result) {
        this(false, true, false, result);
    }

    private ExitReason(ProcessResult result, boolean rethrow) {
        this(rethrow, true, false, result);
    }

    private ExitReason(boolean rethrow, boolean terminating, boolean error, ProcessResult result) {
        this.rethrow = rethrow;
        this.terminating = terminating;
        this.result = result;
        this.error = error;

        if (terminating && result == null) {
            throw new IllegalStateException("All terminating ExitReasons must" + " have a result set");
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

    public boolean isError() {
        return error;
    }
}
