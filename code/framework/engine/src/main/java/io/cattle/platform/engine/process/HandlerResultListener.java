package io.cattle.platform.engine.process;

import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessLogic;

public interface HandlerResultListener {

    HandlerResult onResult(ProcessInstance instance, ProcessState state, ProcessLogic logic,
            ProcessDefinition def, HandlerResult result);

}
