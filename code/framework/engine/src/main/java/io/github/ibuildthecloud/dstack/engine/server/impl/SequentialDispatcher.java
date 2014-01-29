package io.github.ibuildthecloud.dstack.engine.server.impl;

import io.github.ibuildthecloud.dstack.engine.manager.ProcessManager;
import io.github.ibuildthecloud.dstack.engine.server.ProcessInstanceDispatcher;
import io.github.ibuildthecloud.dstack.engine.server.ServerProcessInstanceExecutor;

import javax.inject.Inject;

public class SequentialDispatcher implements ProcessInstanceDispatcher {

    ProcessManager repository;

    @Override
    public void execute(Long id) {
        new ServerProcessInstanceExecutor(id, repository).run();
    }

    public ProcessManager getRepository() {
        return repository;
    }

    @Inject
    public void setRepository(ProcessManager repository) {
        this.repository = repository;
    }

}
