package io.cattle.platform.engine.server.impl;

import io.cattle.platform.engine.manager.ProcessManager;
import io.cattle.platform.engine.server.ProcessServer;
import io.cattle.platform.engine2.model.ProcessReference;

import javax.inject.Inject;

public class ProcessServerImpl implements ProcessServer {

    @Inject
    ProcessManager repository;
    @Inject
    io.cattle.platform.engine2.server.ProcessServer server;

    @Override
    public void runOutstandingJobs() {
        for (ProcessReference ref : repository.pendingTasks()) {
            server.submit(ref);
        }
    }

}
