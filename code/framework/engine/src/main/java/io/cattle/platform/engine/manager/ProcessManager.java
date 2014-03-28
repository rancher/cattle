package io.cattle.platform.engine.manager;

import io.cattle.platform.engine.process.LaunchConfiguration;
import io.cattle.platform.engine.process.ProcessDefinition;
import io.cattle.platform.engine.process.ProcessInstance;

import java.util.List;

public interface ProcessManager {

    List<Long> pendingTasks();

    Long getRemainingTask(long processId);

    ProcessInstance loadProcess(Long id);

    ProcessInstance createProcessInstance(LaunchConfiguration config);

    void scheduleProcessInstance(LaunchConfiguration config);

    void persistState(ProcessInstance process, boolean schedule);

    ProcessDefinition getProcessDelegate(ProcessDefinition def);

    ProcessDefinition getProcessDefinition(String name);

}
