package io.cattle.platform.engine.server;

import io.cattle.platform.engine.manager.ProcessManager;
import io.cattle.platform.engine.process.ProcessInstance;

import org.apache.cloudstack.managed.context.NoExceptionRunnable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerProcessInstanceExecutor extends NoExceptionRunnable {

    private static final Logger log = LoggerFactory.getLogger(ServerProcessInstanceExecutor.class);

    long id;
    ProcessManager repository;

    public ServerProcessInstanceExecutor(long id, ProcessManager repository) {
        super();
        this.id = id;
        this.repository = repository;
    }

    @Override
    public void doRun() {
        ProcessInstance process = repository.loadProcess(id);
        if (process == null) {
            log.error("Failed to find processInstance [{}]", id);
            return;
        }

        process.execute();
    }

}