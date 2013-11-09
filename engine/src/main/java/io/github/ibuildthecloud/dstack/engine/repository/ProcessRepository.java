package io.github.ibuildthecloud.dstack.engine.repository;

import io.github.ibuildthecloud.dstack.engine.process.LaunchConfiguration;
import io.github.ibuildthecloud.dstack.engine.process.ProcessInstance;

public interface ProcessRepository {

//    List<Process> pendingTasks(String id);

//    Process loadProcess(String id);

    ProcessInstance newProcess(LaunchConfiguration config);

    void persistState(ProcessInstance process);

}
