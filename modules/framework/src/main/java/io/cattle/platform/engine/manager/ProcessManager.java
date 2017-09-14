package io.cattle.platform.engine.manager;

import io.cattle.platform.engine.model.ProcessReference;
import io.cattle.platform.engine.process.LaunchConfiguration;
import io.cattle.platform.engine.process.ProcessDefinition;
import io.cattle.platform.engine.process.ProcessInstance;

import java.util.List;

public interface ProcessManager {

    List<ProcessReference> pendingTasks();

    ProcessInstance loadProcess(Long id);

    ProcessInstance createProcessInstance(LaunchConfiguration config);

    void scheduleProcessInstance(LaunchConfiguration config);

    void persistState(ProcessInstance process, boolean schedule);

    ProcessDefinition getProcessDefinition(String name);

}
