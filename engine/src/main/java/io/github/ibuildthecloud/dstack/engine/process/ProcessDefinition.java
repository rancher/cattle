package io.github.ibuildthecloud.dstack.engine.process;

import io.github.ibuildthecloud.dstack.engine.handler.ProcessHandler;
import io.github.ibuildthecloud.dstack.util.type.Named;

import java.util.List;

public interface ProcessDefinition extends Named {

    List<ProcessHandler> getPreProcessHandlers();

    List<ProcessHandler> getProcessHandlers();

    List<ProcessHandler> getPostProcessHandlers();

}
