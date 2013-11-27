package io.github.ibuildthecloud.dstack.engine.process;

import io.github.ibuildthecloud.dstack.engine.handler.ProcessHandler;
import io.github.ibuildthecloud.dstack.engine.handler.ProcessListener;
import io.github.ibuildthecloud.dstack.util.type.Named;

import java.util.List;
import java.util.Set;

public interface ProcessDefinition extends Named {

    List<ProcessListener> getPreProcessListeners();

    List<ProcessHandler> getProcessHandlers();

    List<ProcessListener> getPostProcessListeners();

    ProcessState constructProcessState(LaunchConfiguration config);

    Set<String> getHandlerRequiredResultData();

}
