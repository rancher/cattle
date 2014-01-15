package io.github.ibuildthecloud.dstack.engine.server;

import io.github.ibuildthecloud.dstack.engine.context.EngineContext;
import io.github.ibuildthecloud.dstack.engine.manager.ProcessManager;
import io.github.ibuildthecloud.dstack.engine.process.ProcessInstance;

import org.apache.cloudstack.managed.context.NoExceptionRunnable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerProcessInstanceExecutor extends NoExceptionRunnable {

    private static final Logger log = LoggerFactory.getLogger(ServerProcessInstanceExecutor.class);

    long id;
    ProcessManager repository;
    ProcessServer processServer;

    public ServerProcessInstanceExecutor(long id, ProcessManager repository, ProcessServer processServer) {
        super();
        this.id = id;
        this.repository = repository;
        this.processServer = processServer;
    }

    @Override
    public void doRun() {
        ProcessInstance process = repository.loadProcess(id);
        if ( process == null ) {
            log.error("Failed to find processInstance [{}]", id);
            return;
        }

        EngineContext engineContext = EngineContext.getEngineContext();
        if ( engineContext.getProcessServer() == null )
            engineContext.setProcessServer(processServer);

        process.execute();
    }

}