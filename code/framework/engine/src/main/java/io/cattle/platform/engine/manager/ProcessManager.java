package io.cattle.platform.engine.manager;

import io.cattle.platform.engine.process.LaunchConfiguration;
import io.cattle.platform.engine.process.ProcessDefinition;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.server.ProcessInstanceReference;

import java.util.List;

public interface ProcessManager {

    List<ProcessInstanceReference> pendingTasks();

    Long getRemainingTask(ProcessInstance instance);

    ProcessInstance loadProcess(Long id);

    ProcessInstanceReference loadReference(Long id);

    ProcessInstance createProcessInstance(LaunchConfiguration config);

    void scheduleProcessInstance(LaunchConfiguration config);

    void persistState(ProcessInstance process, boolean schedule);

    ProcessDefinition getProcessDelegate(ProcessDefinition def);

    ProcessDefinition getProcessDefinition(String name);

}
