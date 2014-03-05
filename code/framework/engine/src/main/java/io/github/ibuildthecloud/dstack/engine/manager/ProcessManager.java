package io.github.ibuildthecloud.dstack.engine.manager;

import io.github.ibuildthecloud.dstack.engine.process.LaunchConfiguration;
import io.github.ibuildthecloud.dstack.engine.process.ProcessDefinition;
import io.github.ibuildthecloud.dstack.engine.process.ProcessInstance;

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
