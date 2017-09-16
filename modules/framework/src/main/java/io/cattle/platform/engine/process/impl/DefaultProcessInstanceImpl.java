package io.cattle.platform.engine.process.impl;

import com.google.common.util.concurrent.ListenableFuture;
import com.netflix.config.DynamicLongProperty;
import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.async.utils.AsyncUtils;
import io.cattle.platform.async.utils.TimeoutException;
import io.cattle.platform.deferred.util.DeferredUtils;
import io.cattle.platform.engine.context.EngineContext;
import io.cattle.platform.engine.eventing.ProcessExecuteEvent;
import io.cattle.platform.engine.handler.CompletableLogic;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessHandler;
import io.cattle.platform.engine.idempotent.Idempotent;
import io.cattle.platform.engine.idempotent.IdempotentRetryException;
import io.cattle.platform.engine.manager.ProcessManager;
import io.cattle.platform.engine.manager.impl.ProcessRecord;
import io.cattle.platform.engine.model.Trigger;
import io.cattle.platform.engine.process.ExecutionExceptionHandler;
import io.cattle.platform.engine.process.ExitReason;
import io.cattle.platform.engine.process.LaunchConfiguration;
import io.cattle.platform.engine.process.ProcessDefinition;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessInstanceException;
import io.cattle.platform.engine.process.ProcessResult;
import io.cattle.platform.engine.process.ProcessServiceContext;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.engine.process.ProcessStateTransition;
import io.cattle.platform.engine.process.StateChangeMonitor;
import io.cattle.platform.engine.process.log.ExceptionLog;
import io.cattle.platform.engine.process.log.ParentLog;
import io.cattle.platform.engine.process.log.ProcessExecutionLog;
import io.cattle.platform.engine.process.log.ProcessLog;
import io.cattle.platform.engine.process.log.ProcessLogicExecutionLog;
import io.cattle.platform.lock.LockCallbackNoReturn;
import io.cattle.platform.lock.definition.LockDefinition;
import io.cattle.platform.lock.definition.Namespace;
import io.cattle.platform.lock.exception.FailedToAcquireLockException;
import io.cattle.platform.object.util.ObjectUtils;
import io.cattle.platform.util.exception.ExceptionUtils;
import io.cattle.platform.util.exception.ExecutionException;
import io.cattle.platform.util.exception.LoggableException;
import io.cattle.platform.util.type.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;

import static io.cattle.platform.engine.process.ExitReason.*;
import static io.cattle.platform.util.time.TimeUtils.*;

public class DefaultProcessInstanceImpl implements ProcessInstance {
    private static final DynamicLongProperty RETRY_MAX_WAIT = ArchaiusUtil.getLong("process.retry_max_wait.millis");
    private static final DynamicLongProperty RETRY_RUNNING_DELAY = ArchaiusUtil.getLong("process.running_delay.millis");

    private static final Logger log = LoggerFactory.getLogger(DefaultProcessInstanceImpl.class);

    ProcessServiceContext context;
    ProcessInstanceContext instanceContext;

    ProcessRecord record;
    ProcessLog processLog;
    ProcessExecutionLog execution;
    ExitReason finalReason;
    String chainProcess;
    Date exceptionRunAfter;

    boolean executed = false;
    boolean schedule = false;

    List<ProcessHandler> logic;
    int logicIndex = 0;
    ListenableFuture<?> future;

    public DefaultProcessInstanceImpl(ProcessServiceContext context, ProcessRecord record, ProcessDefinition processDefinition, ProcessState state,
            boolean schedule, boolean replay) {
        super();
        this.schedule = schedule;
        this.context = context;
        this.instanceContext = new ProcessInstanceContext();
        this.instanceContext.setProcessDefinition(processDefinition);
        this.instanceContext.setState(state);
        this.instanceContext.setReplay(replay);
        this.record = record;

        this.processLog = record.getProcessLog();
    }

    @Override
    public ExitReason resume() {
        if (!executed || future == null) {
            throw new IllegalStateException("This process is not suspended");
        }

        executed = false;
        processLog.resume();
        return execute();
    }

    @Override
    public void cancel() {
        if (!executed || future == null) {
            throw new IllegalStateException("This process is not suspended");
        }

        future.cancel(false);
    }

    @Override
    public ExitReason execute() {
        synchronized (this) {
            if (executed) {
                throw new IllegalStateException("A process can only be executed once");
            }
            executed = true;
        }

        if (record.getEndTime() != null) {
            return exit(ALREADY_DONE);
        }

        try {
            return executeWithProcessInstanceLock();
        } finally {
            log.debug("Exiting [{}] process [{}:{}] on resource [{}]", finalReason, getName(), getId(), instanceContext.state.getResourceId());
        }
    }

    protected void openLog(EngineContext engineContext) {
        if (processLog == null) {
            ParentLog parentLog = engineContext.peekLog();
            if (parentLog == null) {
                processLog = new ProcessLog();
            } else {
                processLog = parentLog.newChildLog();
            }
        }

        execution = processLog.newExecution();
        execution.setProcessId(record.getId());
        execution.setResourceType(instanceContext.getProcessDefinition().getResourceType());
        execution.setResourceId(instanceContext.getState().getResourceId());
        execution.setProcessName(instanceContext.getProcessDefinition().getName());

        engineContext.pushLog(execution);
    }

    protected void closeLog(EngineContext engineContext) {
        engineContext.popLog();
    }

    protected ExitReason executeWithProcessInstanceLock() {
        try {
            runAndHandleExceptions(EngineContext.getEngineContext());
            return exit(ExitReason.DONE);
        } catch (ProcessExecutionExitException e) {
            exit(e.getExitReason());
            if (e.getExitReason() != null && e.getExitReason().getResult() == ProcessResult.SUCCESS) {
                return e.getExitReason();
            }

            if (e.getExitReason().isRethrow()) {
                Throwable t = e.getCause();
                ExceptionUtils.rethrowRuntime(t == null ? e : t);
            }

            throw new ProcessInstanceException(this, e);
        } finally {
            context.getProcessManager().persistState(this, schedule);
        }
    }

    protected void trigger() {
        Object resource = instanceContext.getState().getResource();
        Long accountId = ObjectUtils.getAccountId(resource);
        Long clusterId = ObjectUtils.getClusterId(resource);

        for (Trigger trigger : context.getTriggers()) {
            try {
                trigger.trigger(accountId, clusterId, resource, instanceContext.getProcessDefinition().getName());
            } catch (Throwable t) {
                log.error("Exception while running trigger [{}] on [{}:{}]", trigger, getName(), getId(), t);
            }
        }
    }

    protected void runAndHandleExceptions(EngineContext engineContext) {
        try {
            openLog(engineContext);

            preRunStateCheck();

            acquireLockAndRun();
        } catch (ProcessCancelException e) {
            if (!instanceContext.getState().shouldCancel(record) && instanceContext.getState().isTransitioning())
                throw new IllegalStateException("Attempt to cancel when process is still transitioning", e);
            throw e;
        } catch (ProcessExecutionExitException e) {
            e.log(log);
            throw e;
        } catch (IdempotentRetryException e) {
            execution.setException(new ExceptionLog(e));
            throw new ProcessExecutionExitException(RETRY_EXCEPTION, e);
        } catch (TimeoutException t) {
            throw new ProcessExecutionExitException(TIMEOUT, t);
        } catch (Throwable t) {
            execution.setException(new ExceptionLog(t));
            if (t instanceof LoggableException) {
                ((LoggableException) t).log(log);
            } else {
                log.error("Unknown exception", t);
            }
            throw new ProcessExecutionExitException(UNKNOWN_EXCEPTION, t);
        } finally {
            closeLog(engineContext);
        }
    }

    protected void acquireLockAndRun() {
        ProcessState state = instanceContext.getState();

        LockDefinition lockDef = state.getProcessLock();
        if (schedule) {
            lockDef = new Namespace("schedule").getLockDefinition(lockDef);
        }

        try {
            context.getLockManager().lock(lockDef, new LockCallbackNoReturn() {
                @Override
                public void doWithLockNoResult() {
                    runWithProcessLock();
                }
            });
        } catch (FailedToAcquireLockException e) {
            throw new ProcessExecutionExitException(RESOURCE_BUSY, e);
        }
    }

    protected void preRunStateCheck() {
        if (instanceContext.getState().isDone(schedule)) {
            String configuredChainProcess = getConfiguredChainProcess();
            if (configuredChainProcess != null) {
                try {
                    scheduleChain(configuredChainProcess);
                    throw new ProcessExecutionExitException(ExitReason.CHAIN);
                } catch (ProcessCancelException e) {
                }
            }
            throw new ProcessExecutionExitException(ALREADY_DONE);
        }
    }

    protected boolean shouldCancel() {
        return instanceContext.getState().shouldCancel(record) ||
            (instanceContext.isReplay() && !instanceContext.getState().isTransitioning() && instanceContext.getState().isStart(record));
    }

    protected void incrementExecutionCountAndRunAfter() {
        long count = record.getExecutionCount();
        count += 1;

        record.setExecutionCount(count);
        record.setRunAfter(new Date(System.currentTimeMillis() + RETRY_RUNNING_DELAY.get()));
        record.setRunningProcessServerId(EngineContext.getProcessServerId());
    }

    protected void setNextRunAfter() {
        Long count = record.getExecutionCount();
        if (count == 0) {
            count = 1L;
        }

        long wait = RETRY_MAX_WAIT.get();
        double maxCount = Math.ceil(Math.log(RETRY_MAX_WAIT.get())/Math.log(2));
        if (count <= maxCount) {
            wait = Math.min(RETRY_MAX_WAIT.get(), Math.abs(2000L + (long)Math.pow(2, count-1) * 100));
        }
        record.setRunAfter(new Date(System.currentTimeMillis() + wait));
        if (exceptionRunAfter != null && exceptionRunAfter.after(record.getRunAfter())) {
            record.setRunAfter(exceptionRunAfter);
        }
        record.setRunningProcessServerId(null);
    }

    protected void runWithProcessLock() {
        String previousState = null;
        boolean success = false;
        try {
            ProcessState state = instanceContext.getState();
            state.rebuild();

            if (state.getResource() == null) {
                throw new ProcessCancelException("Resource is null [" + getName() + ":" + getId() + "] on resource [" +
                        getResourceId() + "]");
            }

            preRunStateCheck();

            if (shouldCancel()) {
                throw new ProcessCancelException("State [" + instanceContext.getState().getState() + "] is not valid for process [" +
                        getName() + ":" + getId() + "] on resource [" + getResourceId() + "]");
            }

            if (instanceContext.getState().isStart(record)) {
                previousState = setTransitioning();
            }

            if (schedule) {
                runScheduled();
            }

            incrementExecutionCountAndRunAfter();

            previousState = state.getState();

            runLogic();

            boolean chain = setDone(previousState);

            success = true;

            if (chain) {
                throw new ProcessExecutionExitException(ExitReason.CHAIN);
            }
        } catch (ProcessDelayException e) {
            exceptionRunAfter = e.getRunAfter();
            throw e;
        } finally {
            if (!schedule) {
                setNextRunAfter();
            }
            if (!success && !EngineContext.isNestedExecution()) {
                /*
                 * This is not so obvious why we do this. If a process fails it
                 * may have scheduled a compensating process. That means the
                 * state changed under the hood and we should look for that as
                 * it possibly cancels this process. If the process is nested,
                 * we don't want to cancel because it will mask the exception
                 * being thrown and additionally there is no process to cancel
                 * because the process is really owned by the parent. If we were
                 * to cancel a nested process, it will just look like a
                 * RuntimeException to the parent
                 */
                if (previousState != null)
                    assertState(previousState);
            }
        }
    }

    protected void runScheduled() {
        DeferredUtils.defer(new Runnable() {
            @Override
            public void run() {
                Long id = record.getId();
                if (id != null) {
                    ProcessExecuteEvent event = new ProcessExecuteEvent(getProcessRecord());
                    context.getEventService().publish(event);
                }

            }
        });
        record.setRunAfter(null);
        throw new ProcessExecutionExitException(SCHEDULED);
    }

    protected String getConfiguredChainProcess() {
        ProcessState state = instanceContext.getState();
        ProcessDefinition processDefinition = instanceContext.getProcessDefinition();

        if (state.getData().containsKey(processDefinition.getName() + ProcessHandler.CHAIN_PROCESS)) {
            return state.getData().get(processDefinition.getName() + ProcessHandler.CHAIN_PROCESS).toString();
        }

        return null;
    }

    protected void runHandlers() {
        final ProcessDefinition processDefinition = instanceContext.getProcessDefinition();
        final ProcessState state = instanceContext.getState();
        final EngineContext context = EngineContext.getEngineContext();

        for (; logicIndex < logic.size() ; logicIndex++) {
            ProcessHandler handler = logic.get(logicIndex);
            HandlerResult result = idempotentRunHandler(processDefinition, state, context, handler);
            if (result == null) {
                continue;
            }

            result = maybeShortCircuit(result, handler);

            if (result.getFuture() != null) {
                // Save and abort to resume later
                future = result.getFuture();
                throw new ProcessWaitException(future, this);
            }

            String chainResult = result.getChainProcessName();
            if (chainResult == null) {
                continue;
            }

            if (chainProcess != null && !chainResult.equals(chainProcess)) {
                log.error("Not chaining process to [{}] because [{}] already set", chainResult, chainProcess);
            } else {
                chainProcess = chainResult;
            }
        }
    }

    protected HandlerResult idempotentRunHandler(ProcessDefinition processDefinition, ProcessState state, EngineContext context, ProcessHandler handler) {
        try {
            return Idempotent.execute(() -> {
                if (Idempotent.enabled()) {
                    state.reload();
                }
                return runHandler(processDefinition, state, context, handler);
            });
        } finally {
            future = null;
        }
    }

    protected HandlerResult runHandler(ProcessDefinition processDefinition, ProcessState state, EngineContext context, ProcessHandler handler) {
        Named processNamed = new NameWrapper(handler);
        final ProcessLogicExecutionLog processExecution = execution.newProcessLogicExecution(processNamed);
        context.pushLog(processExecution);
        try {
            log.debug("Running handler [{}]", processNamed);
            HandlerResult handlerResult = null;

            if (future == null) {
                handlerResult = handler.handle(state, this);
            } else {
                handlerResult = complete(handler, future, state, this);
            }

            log.debug("Finished handler [{}]", processNamed);

            Map<String, Object> resultData = Collections.emptyMap();
            if (handlerResult != null) {
                resultData = state.convertData(handlerResult.getData());
                processExecution.setChainProcessName(handlerResult.getChainProcessName());
            }

            state.applyData(resultData);

            return handlerResult;
        } catch (ExecutionException e) {
            Idempotent.tempDisable();
            ExecutionExceptionHandler exceptionHandler = this.context.getExceptionHandler();
            if (exceptionHandler != null) {
                exceptionHandler.handleException(e, state, this.context);
            }
            processExecution.setException(new ExceptionLog(e));
            throw e;
        } catch (ProcessCancelException e) {
            throw e;
        } catch (ProcessWaitException e) {
            processExecution.setException(new ExceptionLog(e));
            throw new IllegalStateException("Can not suspend a nested process", e);
        } catch (RuntimeException e) {
            processExecution.setException(new ExceptionLog(e));
            throw e;
        } finally {
            processExecution.setStopTime(now());
            context.popLog();
        }
    }

    protected void runLogic() {
        if (logic == null) {
            ProcessDefinition def = instanceContext.getProcessDefinition();
            logic = def.getProcessHandlers();
        }

        runHandlers();

        String configuredChainProcess = getConfiguredChainProcess();
        if (chainProcess == null && configuredChainProcess != null ) {
            chainProcess = configuredChainProcess;
        }
    }

    protected void assertState(String previousState) {
        ProcessState state = instanceContext.getState();

        state.reload();
        String newState = state.getState();
        if (!previousState.equals(newState)) {
            preRunStateCheck();
            throw new ProcessExecutionExitException("Previous state [" + previousState + "] does not equal current state [" + newState + "]", STATE_CHANGED);
        }
    }

    @Override
    public ProcessRecord getProcessRecord() {
        record.setProcessLog(processLog);
        record.setExitReason(finalReason);

        if (finalReason == null) {
            record.setRunningProcessServerId(EngineContext.getProcessServerId());
        } else {
            if (finalReason == ExitReason.RETRY_EXCEPTION) {
                record.setRunAfter(null);
            }
            record.setRunningProcessServerId(null);
            if (finalReason.isTerminating() && execution != null && execution.getStopTime() != null) {
                record.setEndTime(new Date(execution.getStopTime()));
            }

            record.setResult(finalReason.getResult());
        }

        return record;
    }

    protected String setTransitioning() {
        ProcessState state = instanceContext.getState();

        String previousState = state.getState();
        String newState = state.setTransitioning();
        log.debug("Changing state [{}->{}] on [{}:{}]", previousState, newState, record.getResourceType(), record.getResourceId());
        execution.getTransitions().add(new ProcessStateTransition(previousState, newState, "transitioning", now()));
        publishChanged(previousState, newState, schedule);

        return newState;
    }

    protected void scheduleChain(final String chainProcess) {
        final ProcessState state = instanceContext.getState();
        final LaunchConfiguration config = new LaunchConfiguration(chainProcess,
                record.getResourceType(),
                record.getResourceId(),
                record.getAccountId(),
                record.getClusterId(),
                record.getPriority(),
                state.getData());
        config.setParentProcessState(state);

        ExecutionExceptionHandler handler = this.context.getExceptionHandler();

        Runnable run = new Runnable() {
            @Override
            public void run() {
                DefaultProcessInstanceImpl.this.context.getProcessManager().scheduleProcessInstance(config);
                log.debug("Chained [{}] to [{}]", record.getProcessName(), chainProcess);
                state.reload();
            }
        };

        if (handler == null) {
            run.run();
        } else {
            handler.wrapChainSchedule(state, context, run);
        }
    }

    protected boolean setDone(String previousState) {
        boolean chained = false;
        final ProcessState state = instanceContext.getState();
        assertState(previousState);

        if (chainProcess != null) {
            scheduleChain(chainProcess);
            chained = true;
        }

        String newState = chained ? state.getState() : state.setDone();
        log.debug("Changing state [{}->{}] on [{}:{}]", previousState, newState, record.getResourceType(), record.getResourceId());
        execution.getTransitions().add(new ProcessStateTransition(previousState, newState, chained ? "chain" : "done", now()));
        publishChanged(previousState, newState, schedule);

        return chained;
    }

    protected void publishChanged(String previousState, String newState, boolean defer) {
        if (defer) {
            DeferredUtils.defer(this::trigger);
        } else {
            trigger();
        }

        for (StateChangeMonitor monitor : context.getChangeMonitors()) {
            monitor.onChange(defer, previousState, newState, record, instanceContext.getState(), context);
        }
    }

    protected ExitReason exit(ExitReason reason) {
        if (execution != null) {
            execution.exit(reason);
        }

        return finalReason = reason;
    }

    @Override
    public Long getId() {
        return record.getId();
    }

    @Override
    public String getName() {
        return instanceContext.getProcessDefinition().getName();
    }

    @Override
    public ExitReason getExitReason() {
        return finalReason;
    }

    @Override
    public ProcessManager getProcessManager() {
        return context.getProcessManager();
    }

    @Override
    public String toString() {
        try {
            return "Process [" + instanceContext.getProcessDefinition().getName() + "] resource [" + instanceContext.getState().getResourceId() + "]";
        } catch (NullPointerException e) {
            return super.toString();
        }
    }

    private static HandlerResult maybeShortCircuit(HandlerResult result, ProcessHandler handler) {
        if (handler instanceof CompletableLogic) {
            return result;
        }

        ListenableFuture<?> future = result.getFuture();
        if (future == null) {
            return result;
        }

        if (future.isDone()) {
            return result.withFuture(null);
        }

        return result;
    }

    public static HandlerResult complete(ProcessHandler handler, ListenableFuture<?> future, ProcessState state, ProcessInstance process) {
        try {
            if (handler instanceof CompletableLogic) {
                return ((CompletableLogic) handler).complete(future, state, process);
            }
            AsyncUtils.get(future);
        } catch (CancellationException e) {
            throw new ProcessCancelException(e.getMessage());
        }
        return null;
    }

    @Override
    public String getResourceId() {
        return instanceContext.getState().getResourceId();
    }

    @Override
    public Object getResource() {
        return instanceContext.getState().getResource();
    }

    private static class NameWrapper implements Named {

        Object obj;

        public NameWrapper(Object obj) {
            this.obj = obj;
        }

        @Override
        public String getName() {
            if (obj instanceof Named) {
                return ((Named) obj).getName();
            }
            return obj == null ? "null" : obj.getClass().getSimpleName();
        }
    }
}
