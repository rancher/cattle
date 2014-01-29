package io.github.ibuildthecloud.dstack.engine.process;

import io.github.ibuildthecloud.dstack.engine.handler.HandlerResult;
import io.github.ibuildthecloud.dstack.engine.handler.ProcessLogic;

public interface HandlerResultListener {

    HandlerResult onResult(ProcessInstance instance, ProcessState state, ProcessLogic logic,
            ProcessDefinition def, HandlerResult result);

}
