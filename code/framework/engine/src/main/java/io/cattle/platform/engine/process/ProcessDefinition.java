package io.cattle.platform.engine.process;

import io.cattle.platform.engine.handler.ProcessHandler;
import io.cattle.platform.engine.handler.ProcessPostListener;
import io.cattle.platform.engine.handler.ProcessPreListener;
import io.cattle.platform.util.type.Named;

import java.util.List;

public interface ProcessDefinition extends Named {

    String getResourceType();

    String getProcessDelegateName();

    List<ProcessPreListener> getPreProcessListeners();

    List<ProcessHandler> getProcessHandlers();

    List<ProcessPostListener> getPostProcessListeners();

    ProcessState constructProcessState(LaunchConfiguration config);

    List<StateTransition> getStateTransitions();

}
