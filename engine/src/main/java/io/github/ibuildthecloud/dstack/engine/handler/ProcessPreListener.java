package io.github.ibuildthecloud.dstack.engine.handler;

import io.github.ibuildthecloud.dstack.engine.process.ProcessInstance;
import io.github.ibuildthecloud.dstack.engine.process.ProcessState;
import io.github.ibuildthecloud.dstack.util.type.Named;

public interface ProcessListener extends Named {

    void handle(ProcessState state, ProcessInstance process);

}
