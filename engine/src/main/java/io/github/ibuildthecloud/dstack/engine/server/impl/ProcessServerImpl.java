package io.github.ibuildthecloud.dstack.engine.server.impl;

import javax.inject.Inject;

import io.github.ibuildthecloud.dstack.engine.repository.ProcessManager;
import io.github.ibuildthecloud.dstack.engine.server.ProcessInstanceDispatcher;
import io.github.ibuildthecloud.dstack.engine.server.ProcessServer;

public class ProcessServerImpl implements ProcessServer {

    Long serverId;

    ProcessManager repository;
    ProcessInstanceDispatcher dispatcher;

    @Override
    public Long getId() {
        return serverId;
    }

    @Override
    public void runOutstandingJobs() {
        for ( Long id : repository.pendingTasks() ) {
            dispatcher.execute(this, id);
        }
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
