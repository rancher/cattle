package io.cattle.platform.engine.eventing.impl;

import io.cattle.platform.engine.eventing.ProcessEventListener;
import io.cattle.platform.engine.eventing.ProcessExecuteEvent;
import io.cattle.platform.engine.model.ProcessReference;
import io.cattle.platform.engine.server.ProcessServer;

public class ProcessEventListenerImpl implements ProcessEventListener {

    ProcessServer processServer;

    public ProcessEventListenerImpl(ProcessServer processServer) {
        this.processServer = processServer;
    }

    @Override
    public void processExecute(ProcessExecuteEvent event) {
        if (event.getResourceId() == null || event.getData() == null)
            return;

        processServer.submit(new ProcessReference(new Long(event.getResourceId()), event.getData().getName(), event.getData().getResourceType(),
                event.getData().getResourceId().toString(), event.getData().getAccountId()));
    }

}
