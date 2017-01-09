package io.cattle.platform.engine.process.impl;

import io.cattle.platform.engine.process.ExitReason;

import java.util.Date;

public class ProcessDelayException extends ProcessExecutionExitException {

    private static final long serialVersionUID = -7091071736771973671L;

    Date runAfter;

    public ProcessDelayException(Date runAfter) {
        super("Process delayed until [" + runAfter + "]", ExitReason.DELAY);
        this.runAfter = runAfter;
    }

    public Date getRunAfter() {
        return runAfter;
    }

}
