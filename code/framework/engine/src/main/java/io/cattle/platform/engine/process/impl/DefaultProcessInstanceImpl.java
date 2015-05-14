package io.cattle.platform.engine.process.impl;

import static io.cattle.platform.engine.process.ExitReason.*;
import static io.cattle.platform.engine.process.ProcessInstanceConstants.*;
import static io.cattle.platform.util.time.TimeUtils.*;
import io.cattle.platform.async.utils.TimeoutException;
import io.cattle.platform.deferred.util.DeferredUtils;
import io.cattle.platform.engine.context.EngineContext;
import io.cattle.platform.engine.eventing.EngineEvents;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessHandler;
import io.cattle.platform.engine.handler.ProcessLogic;
import io.cattle.platform.engine.handler.ProcessPostListener;
import io.cattle.platform.engine.handler.ProcessPreListener;
import io.cattle.platform.engine.idempotent.Idempotent;
import io.cattle.platform.engine.idempotent.IdempotentExecution;
import io.cattle.platform.engine.idempotent.IdempotentRetryException;
import io.cattle.platform.engine.manager.ProcessManager;
import io.cattle.platform.engine.manager.impl.ProcessRecord;
import io.cattle.platform.engine.process.ExecutionExceptionHandler;
import io.cattle.platform.engine.process.ExitReason;
import io.cattle.platform.engine.process.LaunchConfiguration;
import io.cattle.platform.engine.process.Predicate;
import io.cattle.platform.engine.process.ProcessDefinition;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessInstanceException;
import io.cattle.platform.engine.process.ProcessPhase;
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
import io.cattle.platform.eventing.model.Event;
import io.cattle.platform.eventing.model.EventVO;
import io.cattle.platform.lock.LockCallback;
import io.cattle.platform.lock.LockCallbackNoReturn;
import io.cattle.platform.lock.definition.DefaultMultiLockDefinition;
import io.cattle.platform.lock.definition.LockDefinition;
import io.cattle.platform.lock.definition.Namespace;
import io.cattle.platform.lock.exception.FailedToAcquireLockException;
import io.cattle.platform.lock.util.LockUtils;
import io.cattle.platform.util.exception.ExceptionUtils;
import io.cattle.platform.util.exception.ExecutionException;
import io.cattle.platform.util.type.CollectionUtils;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultProcessInstanceImpl implements ProcessInstance {

    private static final Logger log = LoggerFactory.getLogger(DefaultProcessInstanceImpl.class);

    ProcessServiceContext context;
    ProcessInstanceContext instanceContext;
    Stack<ProcessInstanceContext> instanceContextHistory = new Stack<ProcessInstanceContext>();

    ProcessRecord record;
    ProcessLog processLog;
    ProcessExecutionLog execution;
    ExitReason finalReason;
    String chainProcess;

    volatile boolean inLogic = false;
    boolean executed = false;
    boolean schedule = false;

    public DefaultProcessInstanceImpl(ProcessServiceContext context, ProcessRecord record, ProcessDefinition processDefinition, ProcessState state,
            boolean schedule) {
        super();
        this.schedule = schedule;
        this.context = context;
        this.instanceContext = new ProcessInstanceContext();
        this.instanceContext.setProcessDefinition(processDefinition);
        this.instanceContext.setState(state);
        this.instanceContext.setPhase(record.getPhase());
        this.record = record;

        this.processLog = record.getProcessLog();
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
            log.info("Attempting to run process [{}:{}] on resource [{}]", getName(), getId(), instanceContext.state.getResourceId());
            return context.getLockManager().lock(new ProcessLock(this), new LockCallback<ExitReason>() {
                @Override
                public ExitReason doWithLock() {
                    return executeWithProcessInstanceLock();
                }
            });
        } catch (FailedToAcquireLockException e) {
            exit(PROCESS_ALREADY_IN_PROGRESS);
            throw new ProcessInstanceException(this, new ProcessExecutionExitException(PROCESS_ALREADY_IN_PROGRESS));
        } finally {
            log.info("Exiting [{}] process [{}:{}] on resource [{}]", finalReason, getName(), getId(), instanceContext.state.getResourceId());
            if (finalReason == null) {
                log.error("final ExitReason is null, should not be");
            }
        }
    }

    protected void lookForCrashes() {
        for (ProcessExecutionLog exec : processLog.getExecutions()) {
            if (exec.getExitReason() == null) {
                exec.setExitReason(SERVER_TERMINATED);
            }
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

        lookForCrashes();

        execution = processLog.newExecution();
        execution.setProcessId(record.getId());
        execution.setResourceType(instanceContext.getProcessDefinition().getResourceType());
        execution.setResourceId(instanceContext.getState().getResourceId());
        execution.setProcessName(instanceContext.getProcessDefinition().getName());

        engineContext.pushLog(execution);
    }

    protected void closeLog(EngineContext engineContext) {
        execution.close();
        engineContext.popLog();
    }

    protected ExitReason executeWithProcessInstanceLock() {
        EngineContext engineContext = EngineContext.getEngineContext();
        try {
            runDelegateLoop(engineContext);

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

    protected void runDelegateLoop(EngineContext engineContext) {
        while (true) {
            try {
                openLog(engineContext);

                preRunStateCheck();

                acquireLockAndRun();

                break;
            } catch (ProcessCancelException e) {
                if (shouldAbort(e)) {
                    if (!instanceContext.getState().shouldCancel(record) && instanceContext.getState().isTransitioning())
                        throw new IllegalStateException("Attempt to cancel when process is still transitioning");
                    throw e;
                } else {
                    execution.exit(DELEGATE);
                }
            } catch (ProcessExecutionExitException e) {
                if (e.getExitReason() != null && e.getExitReason().getResult() == ProcessResult.SUCCESS) {
                    throw e;
                } else if (e.getExitReason().isRethrow()) {
                    if (e.getExitReason().isError()) {
                        log.error("Exiting with code [{}] : {} : [{}]", e.getExitReason(), e.getCause().getClass().getSimpleName(), e.getCause().getMessage());
                    } else {
                        log.info("Exiting with code [{}] : {} : [{}]", e.getExitReason(), e.getCause().getClass().getSimpleName(), e.getCause().getMessage());
                    }
                } else if (e.getExitReason().isError()) {
                    log.error("Exiting with code [{}] : {}", e.getExitReason(), e.getMessage(), e.getCause());
                } else {
                    log.info("Exiting with code [{}] : {}", e.getExitReason(), e.getMessage(), e.getCause());
                }

                throw e;
            } catch (IdempotentRetryException e) {
                execution.setException(new ExceptionLog(e));
                throw new ProcessExecutionExitException(RETRY_EXCEPTION, e);
            } catch (TimeoutException t) {
                throw new ProcessExecutionExitException(TIMEOUT, t);
            } catch (Throwable t) {
                execution.setException(new ExceptionLog(t));
                log.error("Unknown exception", t);
                throw new ProcessExecutionExitException(UNKNOWN_EXCEPTION, t);
            } finally {
                closeLog(engineContext);
            }
        }
    }

    protected boolean shouldAbort(ProcessCancelException e) {
        Boolean skipDelegate = (Boolean)CollectionUtils.getNestedValue(instanceContext.getState().getData(), SKIP_DELEGATE_ON_CANCEL);
        if (skipDelegate != null && skipDelegate) {
            return true;
        }

        ProcessDefinition def = context.getProcessManager().getProcessDelegate(instanceContext.getProcessDefinition());
        if (def == null) {
            return true;
        }

        ProcessState state = def.constructProcessState(record);
        if (state.shouldCancel(record)) {
            return true;
        }

        ProcessInstanceContext newContext = new ProcessInstanceContext();
        newContext.setProcessDefinition(def);
        newContext.setState(state);
        newContext.setPhase(ProcessPhase.STARTED);

        instanceContextHistory.push(instanceContext);
        instanceContext = newContext;

        return false;
    }

    protected void acquireLockAndRun() {
        startLock();
        try {
            context.getLockManager().lock(instanceContext.getProcessLock(), new LockCallbackNoReturn() {
                @Override
                public void doWithLockNoResult() {
                    runWithProcessLock();
                }
            });
        } catch (FailedToAcquireLockException e) {
            lockFailed(e.getLockDefition(), e);
        } finally {
            lockAcquireEnd();
        }
    }

    protected void preRunStateCheck() {
        if (instanceContext.getState().isDone(schedule)) {
            throw new ProcessExecutionExitException(ALREADY_DONE);
        }

        if (instanceContext.getState().shouldCancel(record)) {
            throw new ProcessCancelException("State [" + instanceContext.getState().getState() + "] is not valid");
        }
    }

    protected void runWithProcessLock() {
        String previousState = null;
        boolean success = false;
        try {
            lockAcquired();

            ProcessState state = instanceContext.getState();

            state.reload();

            preRunStateCheck();

            if (schedule) {
                Predicate predicate = record.getPredicate();
                if (predicate != null) {
                    if (!predicate.evaluate(this.instanceContext.getState(), this, this.instanceContext.getProcessDefinition())) {
                        throw new ProcessCancelException("Predicate is not valid");
                    }
                }
            }

            if (instanceContext.getState().isStart(record)) {
                previousState = setTransitioning();
            }

            if (schedule) {
                runScheduled();
            }

            if (instanceContext.getPhase() == ProcessPhase.REQUESTED) {
                instanceContext.setPhase(ProcessPhase.STARTED);
                getProcessManager().persistState(this, schedule);
            }

            previousState = state.getState();

            runLogic();

            boolean chain = setDone();

            success = true;

            if (chain) {
                throw new ProcessExecutionExitException(ExitReason.CHAIN);
            }
        } finally {
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
                    Event event = EventVO.newEvent(EngineEvents.PROCESS_EXECUTE).withResourceId(id.toString());
                    context.getEventService().publish(event);
                }

            }
        });
        throw new ProcessExecutionExitException(SCHEDULED);
    }

    protected boolean runHandlers(ProcessPhase phase, List<? extends ProcessLogic> handlers) {
        boolean shouldDelegate = false;
        boolean ran = false;
        final ProcessDefinition processDefinition = instanceContext.getProcessDefinition();
        final ProcessState state = instanceContext.getState();
        final String previousState = state.getState();
        ProcessPhase currentPhase = instanceContext.getPhase();

        if (instanceContext.getPhase().ordinal() < phase.ordinal()) {
            final EngineContext context = EngineContext.getEngineContext();

            for (final ProcessLogic handler : handlers) {
                ran = true;
                HandlerResult result = Idempotent.execute(new IdempotentExecution<HandlerResult>() {
                    @Override
                    public HandlerResult execute() {
                        return runHandler(processDefinition, state, context, handler);
                    }
                });

                if (result != null) {
                    String chainResult = result.getChainProcessName();

                    if (chainResult != null) {
                        if (chainProcess != null && !chainResult.equals(chainProcess)) {
                            log.error("Not chaining process to [{}] because [{}] already set", chainResult, chainProcess);
                        }
                        chainProcess = chainResult;
                    }

                    shouldDelegate |= result.shouldDelegate();
                    if (!result.shouldContinue(instanceContext.getPhase())) {
                        break;
                    }
                }
            }

            if (currentPhase == ProcessPhase.POST_LISTENERS && chainProcess == null
                    && state.getData().containsKey(processDefinition.getName() + ProcessLogic.CHAIN_PROCESS)) {
                chainProcess = state.getData().get(processDefinition.getName() + ProcessLogic.CHAIN_PROCESS).toString();
            }

            instanceContext.setPhase(phase);
        }

        if (ran) {
            assertState(previousState);
        } else {
            if (phase == ProcessPhase.HANDLER_DONE && processDefinition.getHandlerRequiredResultData().size() > 0) {
                log.error("No handlers ran, but there are required fields to be set");
                throw new ProcessExecutionExitException(MISSING_HANDLER_RESULT_FIELDS);
            }
        }

        return shouldDelegate;
    }

    protected String logicTypeString(ProcessLogic logic) {
        if (logic instanceof ProcessPreListener) {
            return "pre listener ";
        } else if (logic instanceof ProcessHandler) {
            return "handler ";
        } else if (logic instanceof ProcessPostListener) {
            return "post listener ";
        }

        return "";
    }

    protected HandlerResult runHandler(ProcessDefinition processDefinition, ProcessState state, EngineContext context, ProcessLogic handler) {
        final ProcessLogicExecutionLog processExecution = execution.newProcessLogicExecution(handler);
        context.pushLog(processExecution);
        try {
            processExecution.setResourceValueBefore(state.convertData(state.getResource()));

            log.info("Running {}[{}]", logicTypeString(handler), handler.getName());
            HandlerResult handlerResult = handler.handle(state, DefaultProcessInstanceImpl.this);
            log.info("Finished {}[{}]", logicTypeString(handler), handler.getName());

            if (handlerResult == null) {
                return handlerResult;
            }

            boolean shouldContinue = handlerResult.shouldContinue(state.getPhase());

            Map<String, Object> resultData = state.convertData(handlerResult.getData());

            processExecution.setShouldDelegate(handlerResult.shouldDelegate());
            processExecution.setShouldContinue(shouldContinue);
            processExecution.setResultData(resultData);
            processExecution.setChainProcessName(handlerResult.getChainProcessName());

            Set<String> missingFields = new TreeSet<String>();
            processExecution.setMissingRequiredFields(missingFields);

            for (String requiredField : processDefinition.getHandlerRequiredResultData()) {
                if (resultData.get(requiredField) == null) {
                    missingFields.add(requiredField);
                }
            }

            if (!shouldContinue && missingFields.size() > 0) {
                log.error("Missing field [{}] for handler [{}]", missingFields, handler == null ? null : handler.getName());
                throw new ProcessExecutionExitException(MISSING_HANDLER_RESULT_FIELDS);
            }

            if (resultData.size() > 0) {
                state.applyData(resultData);
            }

            processExecution.setResourceValueAfter(state.convertData(state.getResource()));

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
        } catch (RuntimeException e) {
            processExecution.setException(new ExceptionLog(e));
            throw e;
        } finally {
            processExecution.setStopTime(now());
            context.popLog();
        }
    }

    protected void runLogic() {
        inLogic = true;
        boolean shouldDelegate = false;
        try {
            instanceContext.setPhase(ProcessPhase.PRE_LISTENERS);
            shouldDelegate |= runHandlers(ProcessPhase.PRE_LISTENERS_DONE, instanceContext.getProcessDefinition().getPreProcessListeners());

            instanceContext.setPhase(ProcessPhase.HANDLERS);
            shouldDelegate |= runHandlers(ProcessPhase.HANDLER_DONE, instanceContext.getProcessDefinition().getProcessHandlers());

            instanceContext.setPhase(ProcessPhase.POST_LISTENERS);
            shouldDelegate |= runHandlers(ProcessPhase.POST_LISTENERS_DONE, instanceContext.getProcessDefinition().getPostProcessListeners());

            if (shouldDelegate) {
                throw new ProcessCancelException("Process result triggered a delegation");
            }
        } finally {
            inLogic = false;
        }

        instanceContext.setPhase(ProcessPhase.DONE);
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

    public ProcessRecord getProcessRecord() {
        record.setPhase(instanceContext.getPhase());
        record.setProcessLog(processLog);
        record.setExitReason(finalReason);

        if (finalReason == null) {
            record.setRunningProcessServerId(EngineContext.getProcessServerId());
        } else {
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
        log.info("Changing state [{}->{}] on [{}:{}]", previousState, newState, record.getResourceType(), record.getResourceId());
        execution.getTransitions().add(new ProcessStateTransition(previousState, newState, "transitioning", now()));
        publishChanged(previousState, newState, schedule);

        return newState;
    }

    protected boolean setDone() {
        boolean chained = false;
        ProcessState state = instanceContext.getState();
        String previousState = state.getState();

        if (chainProcess != null) {
            LaunchConfiguration config = new LaunchConfiguration(chainProcess, record.getResourceType(), record.getResourceId(), state.getData());
            config.setParentProcessState(state);

            this.context.getProcessManager().scheduleProcessInstance(config);
            log.info("Chained [{}] to [{}]", record.getProcessName(), chainProcess);

            state.reload();
            chained = true;
        }

        String newState = chained ? state.getState() : state.setDone();
        log.info("Changing state [{}->{}] on [{}:{}]", previousState, newState, record.getResourceType(), record.getResourceId());
        execution.getTransitions().add(new ProcessStateTransition(previousState, newState, chained ? "chain" : "done", now()));
        publishChanged(previousState, newState, schedule);

        return chained;
    }

    protected void publishChanged(String previousState, String newState, boolean defer) {
        for (StateChangeMonitor monitor : context.getChangeMonitors()) {
            monitor.onChange(defer, previousState, newState, record, instanceContext.getState(), context);
        }
    }

    protected void startLock() {
        ProcessState state = instanceContext.getState();

        LockDefinition lockDef = state.getProcessLock();
        if (schedule) {
            LockDefinition scheduleLock = new Namespace("schedule").getLockDefinition(lockDef);
            if (record.getPredicate() == null) {
                lockDef = scheduleLock;
            } else {
                lockDef = new DefaultMultiLockDefinition(lockDef, scheduleLock);
            }
        }

        instanceContext.setProcessLock(lockDef);
        execution.setLockAcquireStart(now());
        execution.setProcessLock(LockUtils.serializeLock(instanceContext.getProcessLock()));
    }

    protected void lockAcquired() {
        execution.setLockAcquired(now());
    }

    protected void lockAcquireEnd() {
        if (execution.getLockAcquired() != null)
            execution.setLockAcquireEnd(now());
    }

    protected void lockFailed(LockDefinition lockDef, FailedToAcquireLockException e) {
        execution.setFailedToAcquireLock(LockUtils.serializeLock(lockDef));
        execution.setLockAcquireFailed(now());
        throw new ProcessExecutionExitException(RESOURCE_BUSY, e);
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
    public boolean isRunningLogic() {
        return inLogic;
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

    @Override
    public String getResourceId() {
        return instanceContext.getState().getResourceId();
    }

}
