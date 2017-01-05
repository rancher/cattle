package io.cattle.platform.engine.server.impl;

import io.cattle.platform.engine.manager.ProcessManager;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.server.ProcessInstanceDispatcher;
import io.cattle.platform.engine.server.ProcessInstanceReference;
import io.cattle.platform.engine.server.ProcessServer;

import javax.inject.Inject;

public class ProcessServerImpl implements ProcessServer {

    @Inject
    ProcessManager repository;
    @Inject
    ProcessInstanceDispatcher dispatcher;

    @Override
    public void runOutstandingJobs() {
        for (ProcessInstanceReference ref : repository.pendingTasks()) {
            dispatcher.dispatch(ref);
        }
    }

    @Override
    public Long getNextTask(ProcessInstance instance) {
        return repository.getRemainingTask(instance);
    }

}
