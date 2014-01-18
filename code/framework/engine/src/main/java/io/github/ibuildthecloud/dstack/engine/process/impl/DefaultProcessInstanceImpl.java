package io.github.ibuildthecloud.dstack.engine.process.impl;

import static io.github.ibuildthecloud.dstack.engine.process.ExitReason.*;
import static io.github.ibuildthecloud.dstack.util.time.TimeUtils.*;
import io.github.ibuildthecloud.dstack.deferred.util.DeferredUtils;
import io.github.ibuildthecloud.dstack.engine.context.EngineContext;
import io.github.ibuildthecloud.dstack.engine.eventing.EngineEvents;
import io.github.ibuildthecloud.dstack.engine.handler.HandlerResult;
import io.github.ibuildthecloud.dstack.engine.handler.ProcessHandler;
import io.github.ibuildthecloud.dstack.engine.handler.ProcessLogic;
import io.github.ibuildthecloud.dstack.engine.idempotent.Idempotent;
import io.github.ibuildthecloud.dstack.engine.idempotent.IdempotentExecution;
import io.github.ibuildthecloud.dstack.engine.idempotent.IdempotentRetryException;
import io.github.ibuildthecloud.dstack.engine.manager.ProcessManager;
import io.github.ibuildthecloud.dstack.engine.manager.impl.ProcessRecord;
import io.github.ibuildthecloud.dstack.engine.process.ExitReason;
import io.github.ibuildthecloud.dstack.engine.process.HandlerResultListener;
import io.github.ibuildthecloud.dstack.engine.process.ProcessDefinition;
import io.github.ibuildthecloud.dstack.engine.process.ProcessInstance;
import io.github.ibuildthecloud.dstack.engine.process.ProcessInstanceException;
import io.github.ibuildthecloud.dstack.engine.process.ProcessPhase;
import io.github.ibuildthecloud.dstack.engine.process.ProcessServiceContext;
import io.github.ibuildthecloud.dstack.engine.process.ProcessState;
import io.github.ibuildthecloud.dstack.engine.process.ProcessStateTransition;
import io.github.ibuildthecloud.dstack.engine.process.log.ExceptionLog;
import io.github.ibuildthecloud.dstack.engine.process.log.ParentLog;
import io.github.ibuildthecloud.dstack.engine.process.log.ProcessExecutionLog;
import io.github.ibuildthecloud.dstack.engine.process.log.ProcessLog;
import io.github.ibuildthecloud.dstack.engine.process.log.ProcessLogicExecutionLog;
import io.github.ibuildthecloud.dstack.eventing.model.Event;
import io.github.ibuildthecloud.dstack.eventing.model.EventVO;
import io.github.ibuildthecloud.dstack.lock.LockCallback;
import io.github.ibuildthecloud.dstack.lock.LockCallbackNoReturn;
import io.github.ibuildthecloud.dstack.lock.definition.LockDefinition;
import io.github.ibuildthecloud.dstack.lock.definition.Namespace;
import io.github.ibuildthecloud.dstack.lock.exception.FailedToAcquireLockException;
import io.github.ibuildthecloud.dstack.lock.util.LockUtils;
import io.github.ibuildthecloud.dstack.util.exception.ExceptionUtils;

import java.util.Arrays;
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

    volatile boolean inLogic = false;
    boolean executed = false;
    boolean schedule = false;

    public DefaultProcessInstanceImpl(ProcessServiceContext context, ProcessRecord record, ProcessDefinition processDefinition,
            ProcessState state) {
        super();
        this.context = context;
        this.instanceContext = new ProcessInstanceContext();
        this.instanceContext.setProcessDefinition(processDefinition);
        this.instanceContext.setState(state);
        this.instanceContext.setPhase(record.getPhase());
        this.record = record;

        this.processLog = record.getProcessLog();
    }

    @Override
    public void schedule() {
        schedule = true;
        try {
            execute();
        } catch ( ProcessInstanceException e ) {
            if ( e.getExitReason() == SCHEDULED ) {
                return;
            } else {
                throw e;
            }
        }
    }

    @Override
    public ExitReason execute() {
        synchronized (this) {
            if ( executed ) {
                throw new IllegalStateException("A process can only be executed once");
            }
            executed = true;
        }

        try {
            log.info("Attempting to run process [{}] id [{}] on resource [{}]", getName(),  getId(), instanceContext.state.getResourceId());
            return context.getLockManager().lock(new ProcessLock(this), new LockCallback<ExitReason>() {
                @Override
                public ExitReason doWithLock() {
                    return executeWithProcessInstanceLock();
                }
            });
        } catch ( FailedToAcquireLockException e ) {
            return exit(FAILED_TO_ACQUIRE_PROCESS_INSTANCE_LOCK);
        } finally {
            log.info("Exiting [{}] process [{}] id [{}] on resource [{}]", finalReason, getName(),  getId(), instanceContext.state.getResourceId());
            if ( finalReason == null ) {
                log.error("final ExitReason is null, should not be");
                throw new IllegalStateException("final ExitReason is null, should not be");
            }
        }
    }

    protected void lookForCrashes() {
        for ( ProcessExecutionLog exec : processLog.getExecutions() ) {
            if ( exec.getExitReason() == null ) {
                exec.setExitReason(SERVER_TERMINATED);
            }
        }
    }

    protected void openLog(EngineContext engineContext) {
        if ( processLog == null ) {
            ParentLog parentLog = engineContext.peekLog();
            if ( parentLog == null ) {
                processLog = new ProcessLog();
            } else {
                processLog = parentLog.newChildLog();
            }
        }

        lookForCrashes();

        execution = processLog.newExecution();
        execution.setResourceType(instanceContext.getProcessDefinition().getResourceType());
        execution.setResourceId(instanceContext.getState().getResourceId());
        engineContext.pushLog(execution);

        execution.setProcessName(instanceContext.getProcessDefinition().getName());
    }

    protected void closeLog(EngineContext engineContext) {
        execution.close();
        engineContext.popLog();
    }

    protected ExitReason executeWithProcessInstanceLock() {
        EngineContext engineContext = EngineContext.getEngineContext();
        try {
            try {
                runDelegateLoop(engineContext);
            } catch ( ProcessExecutionExitException e ) {
                throw e;
            } catch ( IdempotentRetryException e ) {
                execution.setException(new ExceptionLog(e));
                throw new ProcessExecutionExitException(RETRY_EXCEPTION, e);
            } catch ( Throwable t) {
                execution.setException(new ExceptionLog(t));
                throw new ProcessExecutionExitException(UNKNOWN_EXCEPTION, t);
            }

            return exit(ExitReason.ACTIVE);
        } catch ( ProcessExecutionExitException e ) {
            exit(e.getExitReason());
            if ( e.getExitReason() == ALREADY_ACTIVE ) {
                return e.getExitReason();
            }

            if ( e.getExitReason().isRethrow() ) {
                if ( e.getExitReason() == RETRY_EXCEPTION ) {
                    log.info("Exiting with code [{}] : {} : [{}]", e.getExitReason(), e.getCause().getClass().getSimpleName(),
                            e.getCause().getMessage());
                } else {
                    log.error("Exiting with code [{}] : {} : [{}]", e.getExitReason(), e.getCause().getClass().getSimpleName(),
                            e.getCause().getMessage());
                }
                ExceptionUtils.rethrowRuntime(e.getCause());
            }

            log.error("Exiting with code [{}]", e.getExitReason(), e.getCause());
            throw new ProcessInstanceException(this, e);
        } finally {
            context.getProcessManager().persistState(this);
        }
    }

    protected void runDelegateLoop(EngineContext engineContext) {
        while ( true ) {
            try {
                openLog(engineContext);

                preRunStateCheck();

                acquireLockAndRun();

                break;
            } catch ( ProcessCancelException e ) {
                if ( shouldAbort(e) ) {
                    if ( ! instanceContext.getState().shouldCancel() && instanceContext.getState().isTransitioning() )
                        throw new IllegalStateException("Attempt to cancel when process is still transitioning");
                    throw e;
                } else {
                    execution.exit(DELEGATE);
                }
            } finally {
                closeLog(engineContext);
            }
        }
    }

    protected boolean shouldAbort(ProcessCancelException e) {
        ProcessDefinition def = context.getProcessManager().getProcessDelegate(instanceContext.getProcessDefinition());
        if ( def == null ) {
            return true;
        }

        ProcessState state = def.constructProcessState(record);
        if ( state.shouldCancel() ) {
            return true;
        }

        ProcessInstanceContext newContext = new ProcessInstanceContext();
        newContext.setPhase(ProcessPhase.STARTED);
        newContext.setProcessDefinition(def);
        newContext.setState(state);

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
        } catch ( FailedToAcquireLockException e ) {
            lockFailed(e.getLockDefition(), e);
        } finally {
            lockAcquireEnd();
        }
    }

    protected void preRunStateCheck() {
        if ( instanceContext.getState().isDone() ) {
            throw new ProcessExecutionExitException(ALREADY_ACTIVE);
        }

        if ( instanceContext.getState().shouldCancel() ) {
            throw new ProcessCancelException();
        }
    }

    protected void runWithProcessLock() {
        boolean success = false;
        try {
            lockAcquired();

            instanceContext.getState().reload();

            preRunStateCheck();

            if ( instanceContext.getPhase() == ProcessPhase.REQUESTED ) {
                instanceContext.setPhase(ProcessPhase.STARTED);
                getProcessManager().persistState(this);
            }

            if ( instanceContext.getState().isStart() ) {
                setTransitioning();
            }

            if ( schedule ) {
                runScheduled();
            }

            runLogic();

            setDone();

            success = true;
        } finally {
            if ( ! success ) {
                assertState();
            }
        }
    }

    protected void runScheduled() {
        DeferredUtils.defer(new Runnable() {
            @Override
            public void run() {
                Long id = record.getId();
                if ( id != null ) {
                    Event event = EventVO.newEvent(EngineEvents.PROCESS_EXECUTE)
                            .withResourceId(id.toString());
                    context.getEventService().publish(event);
                }

            }
        });
        throw new ProcessExecutionExitException(SCHEDULED);
    }

    protected void runListeners(List<? extends ProcessLogic> listeners, ProcessPhase listenerPhase,
            ExitReason exceptionReason) {
        if ( instanceContext.getPhase().ordinal() >= listenerPhase.ordinal() )
            return;

        boolean ran = false;
        EngineContext context = EngineContext.getEngineContext();
        for ( ProcessLogic listener : listeners ) {
            ProcessLogicExecutionLog processExecution = execution.newProcessLogicExecution(listener);
            context.pushLog(processExecution);
            try {
                ran = true;
                listener.handle(instanceContext.getState(), this);
            } catch ( Throwable t ) {
                processExecution.setException(new ExceptionLog(t));
                throw new ProcessExecutionExitException(exceptionReason, t);
            } finally {
                processExecution.setStopTime(now());
                context.popLog();
            }
        }

        if ( ran ) {
            assertState();
        }

        instanceContext.setPhase(listenerPhase);
    }

    protected HandlerResult notifyResult(ProcessHandler handler, ProcessState state, HandlerResult result) {
        for ( HandlerResultListener listener : context.getResultListeners() ) {
            result = listener.onResult(this, state, handler, instanceContext.getProcessDefinition(), result);
        }

        return result;
    }

    protected void runHandlers() {
        boolean ran = false;
        final ProcessDefinition processDefinition = instanceContext.getProcessDefinition();
        final ProcessState state = instanceContext.getState();

        if ( instanceContext.getPhase().ordinal() < ProcessPhase.HANDLER_DONE.ordinal() ) {
            final EngineContext context = EngineContext.getEngineContext();

            List<ProcessHandler> handlers = processDefinition.getProcessHandlers();
            if ( handlers.size() == 0 ) {
                handlers = Arrays.asList((ProcessHandler)null);
            }

            for ( final ProcessHandler handler : handlers ) {
                ran = true;
                Boolean shouldContinue = Idempotent.execute(new IdempotentExecution<Boolean>() {
                    @Override
                    public Boolean execute() {
                        return runHandler(processDefinition, state, context, handler);
                    }
                });

                if ( ! shouldContinue ) {
                    break;
                }
            }

            instanceContext.setPhase(ProcessPhase.HANDLER_DONE);
        }

        if ( ran ) {
            assertState();
        } else {
            if ( processDefinition.getHandlerRequiredResultData().size() > 0 ) {
                log.error("No handlers ran, but there are required fields to be set");
                throw new ProcessExecutionExitException(MISSING_HANDLER_RESULT_FIELDS);
            }
        }
    }

    protected Boolean runHandler(ProcessDefinition processDefinition, ProcessState state,
            EngineContext context, ProcessHandler handler) {
        ProcessLogicExecutionLog processExecution = execution.newProcessLogicExecution(handler);
        context.pushLog(processExecution);
        try {
            processExecution.setResourceValueBefore(state.convertData(state.getResource()));

            HandlerResult result = handler == null ? null : handler.handle(state, DefaultProcessInstanceImpl.this);

            result = notifyResult(handler, state, result);

            if ( result == null ) {
                return true;
            }

            Map<String,Object> resultData = state.convertData(result.getData());

            processExecution.setShouldDelegate(result.shouldDelegate());
            processExecution.setShouldContinue(result.shouldContinue());
            processExecution.setResultData(resultData);

            Set<String> missingFields = new TreeSet<String>();
            processExecution.setMissingRequiredFields(missingFields);

            for ( String requiredField : processDefinition.getHandlerRequiredResultData() ) {
                if ( resultData.get(requiredField) == null ) {
                    missingFields.add(requiredField);
                }
            }

            if ( ! result.shouldContinue() && missingFields.size() > 0 ) {
                log.error("Missing field [{}] for handler [{}]", missingFields, handler == null ? null : handler.getName());
                throw new ProcessExecutionExitException(MISSING_HANDLER_RESULT_FIELDS);
            }

            state.applyData(resultData);
            processExecution.setResourceValueAfter(state.convertData(state.getResource()));

            if ( result.shouldDelegate() ) {
                throw new ProcessCancelException();
            }

            return result.shouldContinue();
        } catch ( ProcessCancelException e ) {
            throw e;
        } catch ( RuntimeException e ) {
            processExecution.setException(new ExceptionLog(e));
            throw e;
        } finally {
            processExecution.setStopTime(now());
            context.popLog();
        }
    }

    protected void runLogic() {
        inLogic = true;
        try {
            runListeners(instanceContext.getProcessDefinition().getPreProcessListeners(), ProcessPhase.PRE_LISTENERS_DONE,
                    ExitReason.PRE_LISTENER_EXCEPTION);

            runHandlers();

            runListeners(instanceContext.getProcessDefinition().getPreProcessListeners(), ProcessPhase.POST_LISTENERS_DONE,
                    ExitReason.POST_LISTENER_EXCEPTION);
        } finally {
            inLogic = false;
        }

        instanceContext.setPhase(ProcessPhase.DONE);
    }

    protected void assertState() {
        ProcessState state = instanceContext.getState();
        String previousState = state.getState();

        state.reload();
        String newState = state.getState();
        if ( ! previousState.equals(newState) ) {
            preRunStateCheck();
            throw new ProcessExecutionExitException(STATE_CHANGED);
        }
    }

    public ProcessRecord getProcessRecord() {
        record.setPhase(instanceContext.getPhase());
        record.setProcessLog(processLog);
        record.setExitReason(finalReason);

        if ( execution == null ) {
            record.setRunningProcessServerId(null);
        } else {
            record.setRunningProcessServerId(execution.getProcessingServerId());
        }

        if ( finalReason != null ) {
            if ( finalReason.isTerminating() && execution != null && execution.getStopTime() != null ) {
                record.setEndTime(new Date(execution.getStopTime()));
            }

            record.setResult(finalReason.getResult());
        }

        return record;
    }

    protected void setTransitioning() {
        ProcessState state = instanceContext.getState();

        String previousState = state.getState();
        String newState = state.setTransitioning();
        execution.getTransitions().add(new ProcessStateTransition(previousState, newState, "transitioning", now()));
    }

    protected void setDone() {
        ProcessState state = instanceContext.getState();

        String previousState = state.getState();
        String newState = state.setDone();
        execution.getTransitions().add(new ProcessStateTransition(previousState, newState, "done", now()));
    }

    protected void startLock() {
        ProcessState state = instanceContext.getState();

        LockDefinition lockDef = state.getProcessLock();
        if ( schedule ) {
            lockDef = new Namespace("schedule").getLockDefinition(lockDef);
        }

        instanceContext.setProcessLock(lockDef);
        execution.setLockAcquireStart(now());
        execution.setProcessLock(LockUtils.serializeLock(instanceContext.getProcessLock()));
    }

    protected void lockAcquired() {
        execution.setLockAcquired(now());
    }

    protected void lockAcquireEnd() {
        if ( execution.getLockAcquired() != null )
            execution.setLockAcquireEnd(now());
    }

    protected void lockFailed(LockDefinition lockDef, FailedToAcquireLockException e) {
        execution.setFailedToAcquireLock(LockUtils.serializeLock(lockDef));
        execution.setLockAcquireFailed(now());
        throw new ProcessExecutionExitException(FAILED_TO_ACQUIRE_LOCK, e);
    }

    protected ExitReason exit(ExitReason reason) {
        if ( execution != null ) {
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

}
