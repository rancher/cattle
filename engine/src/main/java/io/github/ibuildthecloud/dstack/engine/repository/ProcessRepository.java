package io.github.ibuildthecloud.dstack.engine.repository;

import java.util.List;

import io.github.ibuildthecloud.dstack.engine.process.LaunchConfiguration;
import io.github.ibuildthecloud.dstack.engine.process.ProcessInstance;

public interface ProcessRepository {

    List<Long> pendingTasks();

    ProcessInstance loadProcess(Long id);

    ProcessInstance createProcessInstance(LaunchConfiguration config);

    void persistState(ProcessInstance process);

}
