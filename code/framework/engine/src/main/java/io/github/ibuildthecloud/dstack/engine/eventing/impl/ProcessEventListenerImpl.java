package io.github.ibuildthecloud.dstack.engine.eventing.impl;

import io.github.ibuildthecloud.dstack.async.utils.TimeoutException;
import io.github.ibuildthecloud.dstack.engine.eventing.ProcessEventListener;
import io.github.ibuildthecloud.dstack.engine.manager.ProcessManager;
import io.github.ibuildthecloud.dstack.engine.manager.ProcessNotFoundException;
import io.github.ibuildthecloud.dstack.engine.process.ProcessInstanceException;
import io.github.ibuildthecloud.dstack.engine.server.ProcessServer;
import io.github.ibuildthecloud.dstack.eventing.model.Event;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProcessEventListenerImpl implements ProcessEventListener {

    private static final Logger log = LoggerFactory.getLogger(ProcessEventListenerImpl.class);

    ProcessManager processManager;
    ProcessServer processServer;

    @Override
    public void processExecute(Event event) {
        if ( event.getResourceId() == null )
            return;

        long processId = new Long(event.getResourceId());
        boolean runRemaining = false;
        try {
            processManager.loadProcess(processId).execute();
            runRemaining = true;
        } catch ( ProcessNotFoundException e ) {
            log.debug("Failed to find process for id [{}]", event.getResourceId());
        } catch ( ProcessInstanceException e ) {
            log.error("Process [{}] failed, exit [{}] : {}", event.getResourceId(), e.getExitReason(), e.getMessage());
        } catch ( TimeoutException e ) {
            log.info("Communication timeout on process [{}] : {}", event.getResourceId(), e.getMessage());
        } catch ( RuntimeException e ) {
            log.error("Unknown exception running process [{}]", event.getResourceId(), e);
        }

        if ( runRemaining ) {
            processServer.runRemainingTasks(processId);
        }
    }

    public ProcessManager getProcessManager() {
        return processManager;
    }

    @Inject
    public void setProcessManager(ProcessManager processManager) {
        this.processManager = processManager;
    }

    public ProcessServer getProcessServer() {
        return processServer;
    }

    @Inject
    public void setProcessServer(ProcessServer processServer) {
        this.processServer = processServer;
    }

}
