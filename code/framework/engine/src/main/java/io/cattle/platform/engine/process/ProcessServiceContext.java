package io.cattle.platform.engine.process;

import io.cattle.platform.engine.manager.ProcessManager;
import io.cattle.platform.engine.model.Trigger;
import io.cattle.platform.eventing.EventService;
import io.cattle.platform.lock.LockManager;

import java.util.List;

public class ProcessServiceContext {

    LockManager lockManager;
    EventService eventService;
    ProcessManager processManager;
    ExecutionExceptionHandler exceptionHandler;
    List<StateChangeMonitor> changeMonitors;
    List<Trigger> triggers;

    public ProcessServiceContext(LockManager lockManager, EventService eventService, ProcessManager processManager, ExecutionExceptionHandler exceptionHandler,
            List<StateChangeMonitor> changeMonitors, List<Trigger> triggers) {
        super();
        this.lockManager = lockManager;
        this.eventService = eventService;
        this.processManager = processManager;
        this.exceptionHandler = exceptionHandler;
        this.changeMonitors = changeMonitors;
        this.triggers = triggers;
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

    public List<StateChangeMonitor> getChangeMonitors() {
        return changeMonitors;
    }

    public List<Trigger> getTriggers() {
        return triggers;
    }

}
