package io.cattle.platform.engine.eventing.impl;

import io.cattle.platform.engine.eventing.ProcessEventListener;
import io.cattle.platform.engine.manager.ProcessManager;
import io.cattle.platform.engine.server.ProcessInstanceDispatcher;
import io.cattle.platform.engine.server.ProcessInstanceReference;
import io.cattle.platform.eventing.model.Event;

import javax.inject.Inject;

public class ProcessEventListenerImpl implements ProcessEventListener {

    @Inject
    ProcessInstanceDispatcher dispatcher;
    @Inject
    ProcessManager processManager;

    @Override
    public void processExecute(Event event) {
        if (event.getResourceId() == null)
            return;

        ProcessInstanceReference ref = processManager.loadReference(new Long(event.getResourceId()));
        if (ref != null) {
            ref.setEvent(true);
            dispatcher.dispatch(ref);
        }
    }

}
