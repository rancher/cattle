package io.cattle.platform.engine.handler;

import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;

import com.google.common.util.concurrent.ListenableFuture;

public interface CompletableLogic extends ProcessHandler {

    HandlerResult complete(ListenableFuture<?> future, ProcessState state, ProcessInstance process);

}
