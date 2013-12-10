package io.github.ibuildthecloud.dstack.engine.handler;

import io.github.ibuildthecloud.dstack.engine.process.ProcessInstance;
import io.github.ibuildthecloud.dstack.engine.process.ProcessState;
import io.github.ibuildthecloud.dstack.util.type.Named;
import io.github.ibuildthecloud.dstack.util.type.Scope;

public interface ProcessHandler extends Named, Scope {

    HandlerResult handle(ProcessState state, ProcessInstance process);

}
