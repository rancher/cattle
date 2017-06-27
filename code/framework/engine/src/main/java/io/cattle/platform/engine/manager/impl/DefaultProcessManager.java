package io.cattle.platform.engine.manager.impl;

import static io.cattle.platform.engine.process.ExitReason.*;

import io.cattle.platform.engine.context.EngineContext;
import io.cattle.platform.engine.manager.ProcessManager;
import io.cattle.platform.engine.manager.ProcessNotFoundException;
import io.cattle.platform.engine.model.ProcessReference;
import io.cattle.platform.engine.model.Trigger;
import io.cattle.platform.engine.process.ExecutionExceptionHandler;
import io.cattle.platform.engine.process.LaunchConfiguration;
import io.cattle.platform.engine.process.ProcessDefinition;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessInstanceException;
import io.cattle.platform.engine.process.ProcessServiceContext;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.engine.process.StateChangeMonitor;
import io.cattle.platform.engine.process.impl.DefaultProcessInstanceImpl;
import io.cattle.platform.eventing.EventService;
import io.cattle.platform.lock.LockManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class DefaultProcessManager implements ProcessManager {

    ProcessRecordDao processRecordDao;
    LockManager lockManager;
    EventService eventService;
    ExecutionExceptionHandler exceptionHandler;

    Map<String, ProcessDefinition> definitions;
    List<StateChangeMonitor> changeMonitors = new ArrayList<>();
    List<Trigger> triggers = new ArrayList<>();

    public DefaultProcessManager(ProcessRecordDao processRecordDao,
            LockManager lockManager, EventService eventService,
            ExecutionExceptionHandler exceptionHandler,
            StateChangeMonitor changeMonitor,
            Map<String, ProcessDefinition> definitions,
            List<Trigger> triggers) {
        this.processRecordDao = processRecordDao;
        this.lockManager = lockManager;
        this.eventService = eventService;
        this.exceptionHandler = exceptionHandler;
        this.changeMonitors = Arrays.asList(changeMonitor);
        this.definitions = definitions;
        this.triggers = triggers;
    }

    @Override
    public ProcessInstance createProcessInstance(LaunchConfiguration config) {
        return createProcessInstance(new ProcessRecord(config, null, null), false, false);
    }

    @Override
    public void scheduleProcessInstance(LaunchConfiguration config) {
        ProcessInstance pi = createProcessInstance(new ProcessRecord(config, null, null), true, false);
        try {
            pi.execute();
        } catch (ProcessInstanceException e) {
            if (e.getExitReason() == SCHEDULED) {
                return;
            } else {
                throw e;
            }
        }
    }

    protected ProcessInstance createProcessInstance(ProcessRecord record, boolean schedule, boolean replay) {
        if (record == null)
            return null;

        if (record.getRunAfter() != null && record.getRunAfter().after(new Date())) {
            return null;
        }

        ProcessDefinition processDef = definitions.get(record.getProcessName());

        if (processDef == null)
            throw new ProcessNotFoundException("Failed to find ProcessDefinition for [" + record.getProcessName() + "]");

        ProcessState state = processDef.constructProcessState(record);
        if (state == null)
            throw new ProcessNotFoundException("Failed to construct ProcessState for [" + record.getProcessName() + "]");

        if (record.getId() == null && (schedule || !EngineContext.hasParentProcess()))
            record = processRecordDao.insert(record);

        ProcessServiceContext context = new ProcessServiceContext(lockManager, eventService, this, exceptionHandler,
                changeMonitors, triggers);
        DefaultProcessInstanceImpl process = new DefaultProcessInstanceImpl(context, record, processDef, state, schedule, replay);

        return process;
    }

    @Override
    public void persistState(ProcessInstance process, boolean schedule) {
        if (!(process instanceof DefaultProcessInstanceImpl)) {
            throw new IllegalArgumentException("Can only persist ProcessInstances that are created by this repository");
        }

        DefaultProcessInstanceImpl processImpl = (DefaultProcessInstanceImpl) process;

        synchronized (processImpl) {
            ProcessRecord record = processImpl.getProcessRecord();
            if (record.getId() != null)
                processRecordDao.update(record, schedule);
        }
    }

    @Override
    public List<ProcessReference> pendingTasks() {
        return processRecordDao.pendingTasks();
    }

    @Override
    public ProcessInstance loadProcess(Long id) {
        ProcessRecord record = processRecordDao.getRecord(id);
        if (record == null) {
            throw new ProcessNotFoundException("Failed to find ProcessRecord for [" + id + "]");
        }
        return createProcessInstance(record, false, true);
    }

    @Override
    public ProcessDefinition getProcessDefinition(String name) {
        return definitions.get(name);
    }

}
