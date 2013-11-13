package io.github.ibuildthecloud.dstack.engine.server.impl;

import javax.inject.Inject;

import io.github.ibuildthecloud.dstack.engine.repository.ProcessRepository;
import io.github.ibuildthecloud.dstack.engine.server.ProcessInstanceDispatcher;
import io.github.ibuildthecloud.dstack.engine.server.ProcessServer;

public class ProcessServerImpl implements ProcessServer {

    Long serverId;

    ProcessRepository repository;
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

    public ProcessRepository getRepository() {
        return repository;
    }

    @Inject
    public void setRepository(ProcessRepository repository) {
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
