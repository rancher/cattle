package io.github.ibuildthecloud.dstack.engine.handler;

import io.github.ibuildthecloud.dstack.engine.process.ProcessInstance;
import io.github.ibuildthecloud.dstack.util.type.Named;

public interface ProcessHandler extends Named {

    void handle(ProcessInstance process);

}
