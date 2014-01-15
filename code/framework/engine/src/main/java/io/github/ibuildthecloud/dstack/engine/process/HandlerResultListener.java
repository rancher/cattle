package io.github.ibuildthecloud.dstack.engine.process;

import io.github.ibuildthecloud.dstack.engine.handler.HandlerResult;
import io.github.ibuildthecloud.dstack.engine.handler.ProcessHandler;

public interface HandlerResultListener {

    HandlerResult onResult(ProcessInstance instance, ProcessState state, ProcessHandler handler,
            ProcessDefinition def, HandlerResult result);

}
