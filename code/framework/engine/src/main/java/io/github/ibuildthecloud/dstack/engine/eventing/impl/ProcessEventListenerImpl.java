package io.github.ibuildthecloud.dstack.engine.eventing.impl;

import io.github.ibuildthecloud.dstack.engine.eventing.ProcessEventListener;
import io.github.ibuildthecloud.dstack.engine.manager.ProcessManager;
import io.github.ibuildthecloud.dstack.engine.manager.ProcessNotFoundException;
import io.github.ibuildthecloud.dstack.eventing.model.Event;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProcessEventListenerImpl implements ProcessEventListener {

    private static final Logger log = LoggerFactory.getLogger(ProcessEventListenerImpl.class);

    ProcessManager processManager;

    @Override
    public void processExecute(Event event) {
        if ( event.getResourceId() == null )
            return;

        try {
            processManager.loadProcess(new Long(event.getResourceId())).execute();
        } catch ( ProcessNotFoundException e ) {
            log.debug("Failed to find process for id [{}]", event.getResourceId());
        } catch ( RuntimeException e ) {
            log.error("Unknown exception running process [{}]", event.getResourceId(), e);
        }
    }

    public ProcessManager getProcessManager() {
        return processManager;
    }

    @Inject
    public void setProcessManager(ProcessManager processManager) {
        this.processManager = processManager;
    }


}
