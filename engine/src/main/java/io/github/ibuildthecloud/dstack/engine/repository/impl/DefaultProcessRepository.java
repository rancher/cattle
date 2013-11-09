package io.github.ibuildthecloud.dstack.engine.repository.impl;

import io.github.ibuildthecloud.dstack.archaius.util.ArchaiusUtil;
import io.github.ibuildthecloud.dstack.engine.process.LaunchConfiguration;
import io.github.ibuildthecloud.dstack.engine.process.ProcessDefinition;
import io.github.ibuildthecloud.dstack.engine.process.ProcessInstance;
import io.github.ibuildthecloud.dstack.engine.process.ProcessState;
import io.github.ibuildthecloud.dstack.engine.process.ProcessStateFactory;
import io.github.ibuildthecloud.dstack.engine.process.impl.DefaultProcessImpl;
import io.github.ibuildthecloud.dstack.engine.repository.FailedToCreateProcess;
import io.github.ibuildthecloud.dstack.engine.repository.ProcessRepository;
import io.github.ibuildthecloud.dstack.json.JsonMapper;
import io.github.ibuildthecloud.dstack.lock.LockManager;
import io.github.ibuildthecloud.dstack.util.lifecycle.LifeCycle;
import io.github.ibuildthecloud.dstack.util.type.NamedUtils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;

import com.netflix.config.DynamicLongProperty;

public class DefaultProcessRepository implements ProcessRepository, LifeCycle {

    private static final DynamicLongProperty EXECUTION_DELAY = ArchaiusUtil.getLongProperty("process.log.save.interval.ms");

    ProcessRecordDao processRecordDao;
    JsonMapper jsonMapper;
    List<ProcessDefinition> definitions;
    List<ProcessStateFactory> processStateFactories;
    Map<String, ProcessDefinition> definitionsByName;
    Map<String, ProcessStateFactory> factoriesByName;
    LockManager lockManager;
    DelayQueue<Delayed> toPersist;
    ScheduledExecutorService executor;

    @Override
    public ProcessInstance getProcess(LaunchConfiguration config) {
        ProcessDefinition processDef = definitionsByName.get(config.getProcessName());

        if ( processDef == null )
            throw new FailedToCreateProcess("Failed to find ProcessDefinition for [" + processDef.getName() + "]");

        ProcessStateFactory factory = factoriesByName.get(config.getProcessName());
        if ( factory == null )
            throw new FailedToCreateProcess("Failed to find ProcessStateFactory for [" + processDef.getName() + "]");

        ProcessState state = factory.constructProcessState(config);
        if ( state == null )
            throw new FailedToCreateProcess("Failed to construct ProcessState for [" + processDef.getName() + "]");

        return new DefaultProcessImpl(this, lockManager, new ProcessRecord(), processDef, state);
    }

    @Override
    public void persistState(ProcessInstance process) {
        if ( ! ( process instanceof DefaultProcessImpl ) ) {
            throw new IllegalArgumentException("Can only persist ProcessInstances that are created by this repository");
        }

        synchronized (process) {

        }
    }

    @PostConstruct
    public void init() {
        if ( executor == null ) {
            executor = Executors.newSingleThreadScheduledExecutor()();
        }
        definitionsByName = NamedUtils.createMapByName(definitions);
        factoriesByName = NamedUtils.createMapByName(processStateFactories);

        if ( toPersist == null ) {
            toPersist = new DelayQueue<Delayed>();
            executor.
        }
    }

    public List<ProcessDefinition> getDefinitions() {
        return definitions;
    }

    @Inject
    public void setDefinitions(List<ProcessDefinition> definitions) {
        this.definitions = definitions;
    }

    @Override
    public void start() {
        init();
    }

    @PreDestroy
    @Override
    public void stop() {
        if ( executor != null ) {
            executor.shutdownNow();
        }
    }

    public JsonMapper getJsonMapper() {
        return jsonMapper;
    }

    @Inject
    public void setJsonMapper(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
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

}
