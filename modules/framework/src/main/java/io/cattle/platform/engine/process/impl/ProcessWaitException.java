package io.cattle.platform.engine.process.impl;

import io.cattle.platform.engine.process.ExitReason;
import io.cattle.platform.engine.process.ProcessInstance;

import com.google.common.util.concurrent.ListenableFuture;

public class ProcessWaitException extends ProcessExecutionExitException {

    private static final long serialVersionUID = 650893489061276395L;

    ListenableFuture<?> future;
    ProcessInstance processInstance;

    public ProcessWaitException(ListenableFuture<?> future, ProcessInstance processInstance) {
        super(null, ExitReason.WAITING);

        this.processInstance = processInstance;
        this.future = future;
    }

    public ListenableFuture<?> getFuture() {
        return future;
    }

    public ProcessInstance getProcessInstance() {
        return processInstance;
    }

}
