package io.github.ibuildthecloud.dstack.engine.process.impl;

import io.github.ibuildthecloud.dstack.engine.process.ExitReason;

public class ProcessCancelException extends ProcessExecutionExitException {

    private static final long serialVersionUID = -7091071736771973671L;

    public ProcessCancelException() {
        super(ExitReason.CANCELED);
    }

}
