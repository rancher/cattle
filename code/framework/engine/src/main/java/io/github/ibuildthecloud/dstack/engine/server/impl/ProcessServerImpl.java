package io.github.ibuildthecloud.dstack.engine.server.impl;

import io.github.ibuildthecloud.dstack.deferred.util.DeferredUtils;
import io.github.ibuildthecloud.dstack.engine.manager.ProcessManager;
import io.github.ibuildthecloud.dstack.engine.server.ProcessInstanceDispatcher;
import io.github.ibuildthecloud.dstack.engine.server.ProcessServer;

import javax.inject.Inject;

public class ProcessServerImpl implements ProcessServer {

    ProcessManager repository;
    ProcessInstanceDispatcher dispatcher;

    @Override
    public void runOutstandingJobs() {
        for ( Long id : repository.pendingTasks() ) {
            dispatcher.execute(id);
        }
    }

    @Override
    public void runRemainingTasks(long processId) {
        final Long nextId = repository.getRemainingTask(processId);
        if ( nextId != null ) {
            DeferredUtils.defer(new Runnable() {
                @Override
                public void run() {
                    dispatcher.execute(nextId);
                }
            });
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
