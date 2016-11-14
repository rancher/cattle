package io.cattle.platform.engine.eventing.impl;

import io.cattle.platform.engine.eventing.ProcessEventListener;
import io.cattle.platform.engine.server.ProcessInstanceDispatcher;
import io.cattle.platform.eventing.model.Event;

import javax.inject.Inject;

public class ProcessEventListenerImpl implements ProcessEventListener {

    @Inject
    ProcessInstanceDispatcher dispatcher;

    @Override
    public void processExecute(Event event) {
        if (event.getResourceId() == null)
            return;

        dispatcher.execute(new Long(event.getResourceId()), true);
    }

}
