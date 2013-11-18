package io.github.ibuildthecloud.dstack.engine.repository.impl;

import io.github.ibuildthecloud.dstack.archaius.util.ArchaiusUtil;
import io.github.ibuildthecloud.dstack.engine.context.EngineContext;
import io.github.ibuildthecloud.dstack.engine.process.LaunchConfiguration;
import io.github.ibuildthecloud.dstack.engine.process.ProcessDefinition;
import io.github.ibuildthecloud.dstack.engine.process.ProcessInstance;
import io.github.ibuildthecloud.dstack.engine.process.ProcessState;
import io.github.ibuildthecloud.dstack.engine.process.ProcessStateFactory;
import io.github.ibuildthecloud.dstack.engine.process.impl.DefaultProcessImpl;
import io.github.ibuildthecloud.dstack.engine.repository.ProcessNotFoundException;
import io.github.ibuildthecloud.dstack.engine.repository.ProcessManager;
import io.github.ibuildthecloud.dstack.lock.LockManager;
import io.github.ibuildthecloud.dstack.util.concurrent.DelayedObject;
import io.github.ibuildthecloud.dstack.util.init.AfterExtensionInitialization;
import io.github.ibuildthecloud.dstack.util.init.InitializationUtils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.cloudstack.managed.context.NoExceptionRunnable;

import com.netflix.config.DynamicLongProperty;

public class DefaultProcessManager implements ProcessManager {

    private static final DynamicLongProperty EXECUTION_DELAY = ArchaiusUtil.getLongProperty("process.log.save.interval.ms");

    ProcessRecordDao processRecordDao;
    Map<String, ProcessDefinition> definitions;
    Map<String, ProcessStateFactory> factories;
    LockManager lockManager;
    DelayQueue<DelayedObject<DefaultProcessImpl>> toPersist = new DelayQueue<DelayedObject<DefaultProcessImpl>>();
    ScheduledExecutorService executor;

    @Override
    public ProcessInstance createProcessInstance(LaunchConfiguration config) {
        return createProcessInstance(new ProcessRecord(config, null, null));
    }

    protected ProcessInstance createProcessInstance(ProcessRecord record) {
        if ( record == null )
            return null;

        ProcessDefinition processDef = definitions.get(record.getProcessName());

        if ( processDef == null )
            throw new ProcessNotFoundException("Failed to find ProcessDefinition for [" + record.getProcessName() + "]");

        ProcessStateFactory factory = factories.get(record.getProcessName());
        if ( factory == null )
            throw new ProcessNotFoundException("Failed to find ProcessStateFactory for [" + record.getProcessName() + "]");

        ProcessState state = factory.constructProcessState(record);
        if ( state == null )
            throw new ProcessNotFoundException("Failed to construct ProcessState for [" + record.getProcessName() + "]");

        if ( record.getId() == null && ! EngineContext.hasParentProcess() )
            record = processRecordDao.insert(record);

        DefaultProcessImpl process = new DefaultProcessImpl(this, lockManager, record, processDef, state);
        toPersist.put(new DelayedObject<DefaultProcessImpl>(System.currentTimeMillis() + EXECUTION_DELAY.get(), process));

        return process;
    }

    @Override
    public void persistState(ProcessInstance process) {
        if ( ! ( process instanceof DefaultProcessImpl ) ) {
            throw new IllegalArgumentException("Can only persist ProcessInstances that are created by this repository");
        }

        DefaultProcessImpl processImpl = (DefaultProcessImpl)process;

        synchronized (processImpl) {
            ProcessRecord record = processImpl.getProcessRecord();
            if ( record.getId() != null )
                processRecordDao.update(record);
        }
    }

    @Override
    public List<Long> pendingTasks() {
        return processRecordDao.pendingTasks();
    }

    @Override
    public ProcessInstance loadProcess(Long id) {
        ProcessRecord record = processRecordDao.getRecord(id);
        return createProcessInstance(record);
    }
    
    protected void persistInProgress() throws InterruptedException {
        while ( true ) {
            ProcessInstance process = toPersist.take().getObject();
            synchronized (process) {
                if ( process.isRunningLogic() ) {
                    persistState(process);
                }
            }
        }
    }

    @PostConstruct
    public void init() {
        if ( executor == null ) {
            executor = Executors.newSingleThreadScheduledExecutor();
        }

        InitializationUtils.onInitialization(this, definitions);
    }

    @AfterExtensionInitialization
    protected void schedule() {
        executor.scheduleAtFixedRate(new NoExceptionRunnable() {
            @Override
            public void doRun() throws Exception {
                /* This really blocks forever, but just in case it fails we restart */
                persistInProgress();
            }
        }, EXECUTION_DELAY.get(), EXECUTION_DELAY.get(), TimeUnit.MILLISECONDS);        
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

    public void setExecutor(ScheduledExecutorService executor) {
        this.executor = executor;
    }

    public Map<String, ProcessDefinition> getDefinitions() {
        return definitions;
    }

    @Inject
    public void setDefinitions(Map<String, ProcessDefinition> definitions) {
        this.definitions = definitions;
    }

    public Map<String, ProcessStateFactory> getFactories() {
        return factories;
    }

    @Inject
    public void setFactories(Map<String, ProcessStateFactory> factories) {
        this.factories = factories;
    }

}
