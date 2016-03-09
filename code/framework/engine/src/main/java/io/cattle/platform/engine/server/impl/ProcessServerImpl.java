package io.cattle.platform.engine.server.impl;

import io.cattle.platform.engine.manager.ProcessManager;
import io.cattle.platform.engine.server.ProcessInstanceDispatcher;
import io.cattle.platform.engine.server.ProcessServer;

import javax.inject.Inject;

public class ProcessServerImpl implements ProcessServer {

    ProcessManager repository;
    ProcessInstanceDispatcher dispatcher;

    @Override
    public void runOutstandingJobs() {
        for (Long id : repository.pendingTasks()) {
            dispatcher.execute(id);
        }
    }

    @Override
    public Long getRemainingTasks(long processId) {
        return repository.getRemainingTask(processId);
    }

    public ProcessManager getRepository() {
        return repository;
    }

    @Inject
    public void setRepository(ProcessManager repository) {
        this.repository = repository;
    }

    public ProcessInstanceDispatcher getDispatcher() {
        return dispatcher;
    }

    @Inject
    public void setDispatcher(ProcessInstanceDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

}
