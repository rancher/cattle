package io.cattle.platform.engine.eventing.impl;

import io.cattle.platform.engine.eventing.ProcessEventListener;
import io.cattle.platform.engine.eventing.ProcessExecuteEvent;
import io.cattle.platform.engine2.model.ProcessReference;
import io.cattle.platform.engine2.server.ProcessServer;

import javax.inject.Inject;

public class ProcessEventListenerImpl implements ProcessEventListener {

    @Inject
    ProcessServer processServer;

    @Override
    public void processExecute(ProcessExecuteEvent event) {
        if (event.getResourceId() == null || event.getData() == null)
            return;

        processServer.submit(new ProcessReference(new Long(event.getResourceId()), event.getData().getName(), event.getData().getResourceType(),
                event.getData().getResourceId().toString(), event.getData().getAccountId()));
    }

}
