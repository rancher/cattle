package io.github.ibuildthecloud.dstack.engine.manager.impl;

import static io.github.ibuildthecloud.dstack.engine.process.ExitReason.*;
import io.github.ibuildthecloud.dstack.archaius.util.ArchaiusUtil;
import io.github.ibuildthecloud.dstack.engine.context.EngineContext;
import io.github.ibuildthecloud.dstack.engine.manager.ProcessManager;
import io.github.ibuildthecloud.dstack.engine.manager.ProcessNotFoundException;
import io.github.ibuildthecloud.dstack.engine.process.HandlerResultListener;
import io.github.ibuildthecloud.dstack.engine.process.LaunchConfiguration;
import io.github.ibuildthecloud.dstack.engine.process.ProcessDefinition;
import io.github.ibuildthecloud.dstack.engine.process.ProcessInstance;
import io.github.ibuildthecloud.dstack.engine.process.ProcessInstanceException;
import io.github.ibuildthecloud.dstack.engine.process.ProcessServiceContext;
import io.github.ibuildthecloud.dstack.engine.process.ProcessState;
import io.github.ibuildthecloud.dstack.engine.process.impl.DefaultProcessInstanceImpl;
import io.github.ibuildthecloud.dstack.eventing.EventService;
import io.github.ibuildthecloud.dstack.lock.LockManager;
import io.github.ibuildthecloud.dstack.util.concurrent.DelayedObject;
import io.github.ibuildthecloud.dstack.util.type.InitializationTask;
import io.github.ibuildthecloud.dstack.util.type.NamedUtils;

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

    ProcessRecordDao processRecordDao;
    List<ProcessDefinition> definitionList;
    List<HandlerResultListener> listeners;
    Map<String, ProcessDefinition> definitions = new HashMap<String, ProcessDefinition>();
    LockManager lockManager;
    DelayQueue<DelayedObject<WeakReference<ProcessInstance>>> toPersist = new DelayQueue<DelayedObject<WeakReference<ProcessInstance>>>();
    ScheduledExecutorService executor;
    EventService eventService;

    @Override
    public ProcessInstance createProcessInstance(LaunchConfiguration config) {
        return createProcessInstance(new ProcessRecord(config, null, null), false);
    }

    @Override
    public void scheduleProcessInstance(LaunchConfiguration config) {
        ProcessInstance pi = createProcessInstance(new ProcessRecord(config, null, null), true);
        try {
            pi.execute();
        } catch ( ProcessInstanceException e ) {
            if ( e.getExitReason() == SCHEDULED ) {
                return;
            } else {
                throw e;
            }
        }
    }

    protected ProcessInstance createProcessInstance(ProcessRecord record, boolean schedule) {
        if ( record == null )
            return null;

        ProcessDefinition processDef = definitions.get(record.getProcessName());

        if ( processDef == null )
            throw new ProcessNotFoundException("Failed to find ProcessDefinition for [" + record.getProcessName() + "]");

        ProcessState state = processDef.constructProcessState(record);
        if ( state == null )
            throw new ProcessNotFoundException("Failed to construct ProcessState for [" + record.getProcessName() + "]");

        if ( record.getId() == null && (schedule || ! EngineContext.hasParentProcess()) )
            record = processRecordDao.insert(record);

        ProcessServiceContext context = new ProcessServiceContext(lockManager, eventService, this, listeners);
        DefaultProcessInstanceImpl process = new DefaultProcessInstanceImpl(context, record, processDef, state, schedule);

        if ( record.getId() != null )
            queue(process);

        return process;
    }


    @Override
    public ProcessDefinition getProcessDelegate(ProcessDefinition def) {
        String delegate = def.getProcessDelegateName();

        if ( delegate == null ) {
            return null;
        }

        ProcessDefinition processDef = definitions.get(delegate);

        if ( processDef == null )
            throw new ProcessNotFoundException("Failed to find ProcessDefinition for [" + delegate + "]");

        return processDef;
    }

    @Override
    public void persistState(ProcessInstance process, boolean schedule) {
        if ( ! ( process instanceof DefaultProcessInstanceImpl ) ) {
            throw new IllegalArgumentException("Can only persist ProcessInstances that are created by this repository");
        }

        DefaultProcessInstanceImpl processImpl = (DefaultProcessInstanceImpl)process;

        synchronized (processImpl) {
            ProcessRecord record = processImpl.getProcessRecord();
            if ( record.getId() != null )
                processRecordDao.update(record, schedule);
        }
    }

    @Override
    public List<Long> pendingTasks() {
        return processRecordDao.pendingTasks(null, null);
    }

    @Override
    public Long getRemainingTask(long processId) {
        ProcessRecord record = processRecordDao.getRecord(processId);
        if ( record == null ) {
            return null;
        }

        List<Long> next = processRecordDao.pendingTasks(record.getResourceType(), record.getResourceId());
        return next.size() == 0 ? null : next.get(0);
    }

    @Override
    public ProcessInstance loadProcess(Long id) {
        ProcessRecord record = processRecordDao.getRecord(id);
        if ( record == null ) {
            throw new ProcessNotFoundException("Failed to find ProcessRecord for [" + id + "]");
        }
        return createProcessInstance(record, false);
    }

    protected void persistInProgress() throws InterruptedException {
        while ( true ) {
            ProcessInstance process = toPersist.take().getObject().get();
            if ( process == null ) {
                return;
            }

            synchronized (process) {
                if ( process.isRunningLogic() ) {
                    persistState(process, false);
                }
                if ( process.getExitReason() == null ) {
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
                /* This really blocks forever, but just in case it fails we restart */
                persistInProgress();
            }
        }, EXECUTION_DELAY.get(), EXECUTION_DELAY.get(), TimeUnit.MILLISECONDS);
    }

    @Override
    public void stop() {
    }

    public ProcessRecordDao getProcessRecordDao() {
        return processRecordDao;
    }

    @Inject
    public void setProcessRecordDao(ProcessRecordDao processRecordDao) {
        this.processRecordDao = processRecordDao;
    }

    public LockManager getLockManager() {
        return lockManager;
    }

    @Inject
    public void setLockManager(LockManager lockManager) {
        this.lockManager = lockManager;
    }

    public ScheduledExecutorService getExecutor() {
        return executor;
    }

    @Inject
    public void setExecutor(ScheduledExecutorService executor) {
        this.executor = executor;
    }

    public List<ProcessDefinition> getDefinitionList() {
        return definitionList;
    }

    @Inject
    public void setDefinitionList(List<ProcessDefinition> definitionList) {
        this.definitionList = definitionList;
    }

    public EventService getEventService() {
        return eventService;
    }

    @Inject
    public void setEventService(EventService eventService) {
        this.eventService = eventService;
    }

    public List<HandlerResultListener> getListeners() {
        return listeners;
    }

    @Inject
    public void setListeners(List<HandlerResultListener> listeners) {
        this.listeners = listeners;
    }

}
