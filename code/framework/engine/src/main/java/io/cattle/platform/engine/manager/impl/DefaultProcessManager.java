package io.cattle.platform.engine.manager.impl;

import static io.cattle.platform.engine.process.ExitReason.*;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.engine.context.EngineContext;
import io.cattle.platform.engine.manager.ProcessManager;
import io.cattle.platform.engine.manager.ProcessNotFoundException;
import io.cattle.platform.engine.process.ExecutionExceptionHandler;
import io.cattle.platform.engine.process.LaunchConfiguration;
import io.cattle.platform.engine.process.ProcessDefinition;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessInstanceException;
import io.cattle.platform.engine.process.ProcessServiceContext;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.engine.process.StateChangeMonitor;
import io.cattle.platform.engine.process.impl.DefaultProcessInstanceImpl;
import io.cattle.platform.engine.server.ProcessInstanceReference;
import io.cattle.platform.eventing.EventService;
import io.cattle.platform.lock.LockManager;
import io.cattle.platform.util.concurrent.DelayedObject;
import io.cattle.platform.util.type.InitializationTask;
import io.cattle.platform.util.type.NamedUtils;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.apache.cloudstack.managed.context.NoExceptionRunnable;

import com.netflix.config.DynamicLongProperty;

public class DefaultProcessManager implements ProcessManager, InitializationTask {

    private static final DynamicLongProperty EXECUTION_DELAY = ArchaiusUtil.getLong("process.log.save.interval.ms");

    @Inject
    ProcessRecordDao processRecordDao;
    @Inject
    List<ProcessDefinition> definitionList;
    Map<String, ProcessDefinition> definitions = new HashMap<String, ProcessDefinition>();
    @Inject
    LockManager lockManager;
    DelayQueue<DelayedObject<WeakReference<ProcessInstance>>> toPersist = new DelayQueue<DelayedObject<WeakReference<ProcessInstance>>>();
    @Inject
    ScheduledExecutorService executor;
    @Inject
    EventService eventService;
    @Inject
    ExecutionExceptionHandler exceptionHandler;
    @Inject
    List<StateChangeMonitor> changeMonitors;

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

        ProcessDefinition processDef = definitions.get(record.getProcessName());

        if (processDef == null)
            throw new ProcessNotFoundException("Failed to find ProcessDefinition for [" + record.getProcessName() + "]");

        ProcessState state = processDef.constructProcessState(record);
        if (state == null)
            throw new ProcessNotFoundException("Failed to construct ProcessState for [" + record.getProcessName() + "]");

        if (record.getId() == null && (schedule || !EngineContext.hasParentProcess()))
            record = processRecordDao.insert(record);

        ProcessServiceContext context = new ProcessServiceContext(lockManager, eventService, this, exceptionHandler, changeMonitors);
        DefaultProcessInstanceImpl process = new DefaultProcessInstanceImpl(context, record, processDef, state, schedule, replay);

        if (record.getId() != null)
            queue(process);

        return process;
    }

    @Override
    public ProcessDefinition getProcessDelegate(ProcessDefinition def) {
        String delegate = def.getProcessDelegateName();

        if (delegate == null) {
            return null;
        }

        ProcessDefinition processDef = definitions.get(delegate);

        if (processDef == null)
            throw new ProcessNotFoundException("Failed to find ProcessDefinition for [" + delegate + "]");

        return processDef;
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
    public List<ProcessInstanceReference> pendingTasks() {
        return processRecordDao.pendingTasks();
    }


    @Override
    public Long getRemainingTask(ProcessInstance instance) {
        if (!(instance instanceof DefaultProcessInstanceImpl)) {
            return null;
        }
        ProcessRecord record = ((DefaultProcessInstanceImpl)instance).getProcessRecord();
        if (record == null) {
            return null;
        }

        return processRecordDao.nextTask(record.getResourceType(), record.getResourceId());
    }

    @Override
    public ProcessInstance loadProcess(Long id) {
        ProcessRecord record = processRecordDao.getRecord(id);
        if (record == null) {
            throw new ProcessNotFoundException("Failed to find ProcessRecord for [" + id + "]");
        }
        return createProcessInstance(record, false, true);
    }

    protected void persistInProgress() throws InterruptedException {
        while (true) {
            ProcessInstance process = toPersist.take().getObject().get();
            if (process == null) {
                return;
            }

            synchronized (process) {
                if (process.isRunningLogic()) {
                    persistState(process, false);
                }
                if (process.getExitReason() == null) {
                    queue(process);
                }
            }
        }
    }

    protected void queue(ProcessInstance process) {
        WeakReference<ProcessInstance> ref = new WeakReference<ProcessInstance>(process);
        toPersist.put(new DelayedObject<WeakReference<ProcessInstance>>(System.currentTimeMillis() + EXECUTION_DELAY.get(), ref));
    }

    @Override
    public ProcessDefinition getProcessDefinition(String name) {
        return definitions.get(name);
    }

    @Override
    public void start() {
        definitions = NamedUtils.createMapByName(definitionList);

        executor.scheduleAtFixedRate(new NoExceptionRunnable() {
            @Override
            public void doRun() throws Exception {
                /*
                 * This really blocks forever, but just in case it fails we
                 * restart
                 */
                persistInProgress();
            }
        }, EXECUTION_DELAY.get(), EXECUTION_DELAY.get(), TimeUnit.MILLISECONDS);
    }

    @Override
    public ProcessInstanceReference loadReference(Long id) {
        return processRecordDao.loadReference(id);
    }

}
