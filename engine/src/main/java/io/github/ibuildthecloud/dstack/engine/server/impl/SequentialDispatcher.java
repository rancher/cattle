package io.github.ibuildthecloud.dstack.engine.server.impl;

import javax.inject.Inject;

import io.github.ibuildthecloud.dstack.engine.repository.ProcessRepository;
import io.github.ibuildthecloud.dstack.engine.server.ProcessInstanceDispatcher;
import io.github.ibuildthecloud.dstack.engine.server.ProcessServer;
import io.github.ibuildthecloud.dstack.engine.server.ServerProcessInstanceExecutor;

public class SequentialDispatcher implements ProcessInstanceDispatcher {

    ProcessRepository repository;

    @Override
    public void execute(ProcessServer server, Long id) {
        new ServerProcessInstanceExecutor(id, repository, server).run();
    }

    public ProcessRepository getRepository() {
        return repository;
    }

    @Inject
    public void setRepository(ProcessRepository repository) {
        this.repository = repository;
    }

}
