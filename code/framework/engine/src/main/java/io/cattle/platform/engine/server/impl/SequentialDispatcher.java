package io.cattle.platform.engine.server.impl;

import io.cattle.platform.engine.manager.ProcessManager;
import io.cattle.platform.engine.server.ProcessInstanceDispatcher;
import io.cattle.platform.engine.server.ServerProcessInstanceExecutor;

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
