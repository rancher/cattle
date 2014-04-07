package io.cattle.platform.engine.process;

import io.cattle.platform.engine.manager.ProcessManager;
import io.cattle.platform.eventing.EventService;
import io.cattle.platform.lock.LockManager;

public class ProcessServiceContext {

    LockManager lockManager;
    EventService eventService;
    ProcessManager processManager;
    ExecutionExceptionHandler exceptionHandler;

    public ProcessServiceContext(LockManager lockManager, EventService eventService, ProcessManager processManager,
            ExecutionExceptionHandler exceptionHandler) {
        super();
        this.lockManager = lockManager;
        this.eventService = eventService;
        this.processManager = processManager;
        this.exceptionHandler = exceptionHandler;
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

    public ExecutionExceptionHandler getExceptionHandler() {
        return exceptionHandler;
    }

}
