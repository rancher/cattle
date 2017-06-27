package io.cattle.platform.engine.handler;

import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;

public interface ProcessHandler {

    public static final String CHAIN_PROCESS = "::chain";

    HandlerResult handle(ProcessState state, ProcessInstance process);

}
