package io.github.ibuildthecloud.dstack.engine.handler;

import io.github.ibuildthecloud.dstack.engine.process.ProcessInstance;
import io.github.ibuildthecloud.dstack.engine.process.ProcessState;
import io.github.ibuildthecloud.dstack.util.type.Named;

public interface ProcessHandler<T> extends Named {

    HandlerResult handle(ProcessState<T> state, ProcessInstance process);

}
