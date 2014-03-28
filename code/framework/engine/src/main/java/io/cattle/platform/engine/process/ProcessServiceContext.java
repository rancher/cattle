package io.cattle.platform.engine.process;

import io.cattle.platform.engine.manager.ProcessManager;
import io.cattle.platform.eventing.EventService;
import io.cattle.platform.lock.LockManager;

import java.util.List;

public class ProcessServiceContext {

    LockManager lockManager;
    EventService eventService;
    ProcessManager processManager;

    public ProcessServiceContext(LockManager lockManager, EventService eventService, ProcessManager processManager,
            List<HandlerResultListener> listeners) {
        super();
        this.lockManager = lockManager;
        this.eventService = eventService;
        this.processManager = processManager;
    }

    public LockManager getLockManager() {
        return lockManager;
    }

    public EventService getEventService() {
        return eventService;
    }

    public ProcessManager getProcessManager() {
        return processManager;
    }

}
