package io.github.ibuildthecloud.dstack.engine.process;

import io.github.ibuildthecloud.dstack.engine.handler.ProcessHandler;
import io.github.ibuildthecloud.dstack.engine.handler.ProcessPostListener;
import io.github.ibuildthecloud.dstack.engine.handler.ProcessPreListener;
import io.github.ibuildthecloud.dstack.util.type.Named;

import java.util.List;
import java.util.Set;

public interface ProcessDefinition extends Named {

    String getResourceType();

    String getProcessDelegateName();

    List<ProcessPreListener> getPreProcessListeners();

    List<ProcessHandler> getProcessHandlers();

    List<ProcessPostListener> getPostProcessListeners();

    ProcessState constructProcessState(LaunchConfiguration config);

    Set<String> getHandlerRequiredResultData();

    List<StateTransition> getStateTransitions();

}
