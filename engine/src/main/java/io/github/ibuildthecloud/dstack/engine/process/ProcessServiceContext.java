package io.github.ibuildthecloud.dstack.engine.process;

import io.github.ibuildthecloud.dstack.engine.manager.ProcessManager;
import io.github.ibuildthecloud.dstack.eventing.EventService;
import io.github.ibuildthecloud.dstack.lock.LockManager;

import java.util.List;

public class ProcessServiceContext {

    List<HandlerResultListener> resultListeners;
    LockManager lockManager;
    EventService eventService;
    ProcessManager processManager;

    public ProcessServiceContext(LockManager lockManager, EventService eventService, ProcessManager processManager,
            List<HandlerResultListener> listeners) {
        super();
        this.lockManager = lockManager;
        this.eventService = eventService;
        this.processManager = processManager;
        this.resultListeners = listeners;
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

    public List<HandlerResultListener> getResultListeners() {
        return resultListeners;
    }
}
