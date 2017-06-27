package io.cattle.platform.engine.process;

import io.cattle.platform.engine.handler.ProcessHandler;
import io.cattle.platform.util.type.Named;

import java.util.List;

public interface ProcessDefinition extends Named {

    String getResourceType();

    List<ProcessHandler> getProcessHandlers();

    ProcessState constructProcessState(LaunchConfiguration config);

    List<StateTransition> getStateTransitions();

}
