package io.cattle.platform.engine.server.impl;

import io.cattle.platform.engine.manager.ProcessManager;
import io.cattle.platform.engine.server.ProcessInstanceDispatcher;
import io.cattle.platform.engine.server.ProcessServer;

import javax.inject.Inject;

public class ProcessServerImpl implements ProcessServer {

    @Inject
    ProcessManager repository;
    @Inject
    ProcessInstanceDispatcher dispatcher;

    @Override
    public void runOutstandingJobs() {
        for (Long id : repository.pendingTasks()) {
            dispatcher.execute(id, false);
        }
        for (Long id : repository.pendingPriorityTasks()) {
            dispatcher.execute(id, true);
        }
    }

    @Override
    public Long getRemainingTasks(long processId) {
        return repository.getRemainingTask(processId);
    }

}
