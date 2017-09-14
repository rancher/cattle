package io.cattle.platform.engine.process.impl;

import io.cattle.platform.engine.process.ExitReason;

public class ProcessCancelException extends ProcessExecutionExitException {

    private static final long serialVersionUID = -7091071736771973671L;

    public ProcessCancelException(String message) {
        super(message, ExitReason.CANCELED);
    }

}
