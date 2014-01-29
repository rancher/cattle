package io.github.ibuildthecloud.dstack.engine.process.impl;

import static io.github.ibuildthecloud.dstack.engine.process.ExitReason.*;
import static io.github.ibuildthecloud.dstack.util.time.TimeUtils.*;
import io.github.ibuildthecloud.dstack.deferred.util.DeferredUtils;
import io.github.ibuildthecloud.dstack.engine.context.EngineContext;
import io.github.ibuildthecloud.dstack.engine.eventing.EngineEvents;
import io.github.ibuildthecloud.dstack.engine.handler.HandlerResult;
import io.github.ibuildthecloud.dstack.engine.handler.ProcessHandler;
import io.github.ibuildthecloud.dstack.engine.handler.ProcessLogic;
import io.github.ibuildthecloud.dstack.engine.handler.ProcessPostListener;
import io.github.ibuildthecloud.dstack.engine.handler.ProcessPreListener;
import io.github.ibuildthecloud.dstack.engine.idempotent.Idempotent;
import io.github.ibuildthecloud.dstack.engine.idempotent.IdempotentExecution;
import io.github.ibuildthecloud.dstack.engine.idempotent.IdempotentRetryException;
import io.github.ibuildthecloud.dstack.engine.manager.ProcessManager;
import io.github.ibuildthecloud.dstack.engine.manager.impl.ProcessRecord;
import io.github.ibuildthecloud.dstack.engine.process.ExitReason;
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
import io.github.ibuildthecloud.dstack.framework.event.FrameworkEvents;
import io.github.ibuildthecloud.dstack.lock.LockCallback;
import io.github.ibuildthecloud.dstack.lock.LockCallbackNoReturn;
import io.github.ibuildthecloud.dstack.lock.definition.LockDefinition;
import io.github.ibuildthecloud.dstack.lock.definition.Namespace;
import io.github.ibuildthecloud.dstack.lock.exception.FailedToAcquireLockException;
import io.github.ibuildthecloud.dstack.lock.util.LockUtils;
import io.github.ibuildthecloud.dstack.util.exception.ExceptionUtils;

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
            exit(FAILED_TO_ACQUIRE_PROCESS_INSTANCE_LOCK);
            throw new ProcessInstanceException(this, new ProcessExecutionExitException(FAILED_TO_ACQUIRE_PROCESS_INSTANCE_LOCK));
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

            if ( e.getExitReason() == SCHEDULED ) {
                log.info("Exiting with code [{}]", e.getExitReason(), e.getCause());
            } else {
                log.error("Exiting with code [{}]", e.getExitReason(), e.getCause());
            }
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
            } catch ( ProcessExecutionExitException e ) {
                throw e;
            } catch ( IdempotentRetryException e ) {
                execution.setException(new ExceptionLog(e));
                throw new ProcessExecutionExitException(RETRY_EXCEPTION, e);
            } catch ( Throwable t) {
                execution.setException(new ExceptionLog(t));
                throw new ProcessExecutionExitException(UNKNOWN_EXCEPTION, t);
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
            throw new ProcessCancelException("State [" + instanceContext.getState().getState() + "] is not valid");
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

    protected boolean runHandlers(ProcessPhase phase, List<? extends ProcessLogic> handlers) {
        boolean shouldDelegate = false;
        boolean ran = false;
        final ProcessDefinition processDefinition = instanceContext.getProcessDefinition();
        final ProcessState state = instanceContext.getState();

        if ( instanceContext.getPhase().ordinal() < phase.ordinal() ) {
            final EngineContext context = EngineContext.getEngineContext();

            for ( final ProcessLogic handler : handlers ) {
                ran = true;
                HandlerResult result = Idempotent.execute(new IdempotentExecution<HandlerResult>() {
                    @Override
                    public HandlerResult execute() {
                        return runHandler(processDefinition, state, context, handler);
                    }
                });

                if ( result != null ) {
                    shouldDelegate |= result.shouldDelegate();
                    if ( ! result.shouldContinue(instanceContext.getPhase()) ) {
                        break;
                    }
                }
            }

            instanceContext.setPhase(phase);
        }

        if ( ran ) {
            assertState();
        } else {
            if ( phase == ProcessPhase.HANDLER_DONE && processDefinition.getHandlerRequiredResultData().size() > 0 ) {
                log.error("No handlers ran, but there are required fields to be set");
                throw new ProcessExecutionExitException(MISSING_HANDLER_RESULT_FIELDS);
            }
        }

        return shouldDelegate;
    }

    protected String logicTypeString(ProcessLogic logic) {
        if ( logic instanceof ProcessPreListener ) {
            return "pre listener ";
        } else if ( logic instanceof ProcessHandler ) {
            return "handler ";
        } else if ( logic instanceof ProcessPostListener ) {
            return "post listener ";
        }

        return "";
    }

    protected HandlerResult runHandler(ProcessDefinition processDefinition, ProcessState state,
            EngineContext context, ProcessLogic handler) {
        ProcessLogicExecutionLog processExecution = execution.newProcessLogicExecution(handler);
        context.pushLog(processExecution);
        try {
            processExecution.setResourceValueBefore(state.convertData(state.getResource()));

            log.info("Running {}[{}]", logicTypeString(handler), handler.getName());
            HandlerResult handlerResult = handler.handle(state, DefaultProcessInstanceImpl.this);
            log.info("Finished {}[{}]", logicTypeString(handler), handler.getName());

            if ( handlerResult == null ) {
                return handlerResult;
            }

            boolean shouldContinue = handlerResult.shouldContinue(state.getPhase());

            Map<String,Object> resultData = state.convertData(handlerResult.getData());

            processExecution.setShouldDelegate(handlerResult.shouldDelegate());
            processExecution.setShouldContinue(shouldContinue);
            processExecution.setResultData(resultData);

            Set<String> missingFields = new TreeSet<String>();
            processExecution.setMissingRequiredFields(missingFields);

            for ( String requiredField : processDefinition.getHandlerRequiredResultData() ) {
                if ( resultData.get(requiredField) == null ) {
                    missingFields.add(requiredField);
                }
            }

            if ( ! shouldContinue && missingFields.size() > 0 ) {
                log.error("Missing field [{}] for handler [{}]", missingFields, handler == null ? null : handler.getName());
                throw new ProcessExecutionExitException(MISSING_HANDLER_RESULT_FIELDS);
            }

            state.applyData(resultData);
            processExecution.setResourceValueAfter(state.convertData(state.getResource()));

            return handlerResult;
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
        boolean shouldDelegate = false;
        try {
            instanceContext.setPhase(ProcessPhase.PRE_LISTENERS);
            shouldDelegate |= runHandlers(ProcessPhase.PRE_LISTENERS_DONE, instanceContext.getProcessDefinition().getPreProcessListeners());

            instanceContext.setPhase(ProcessPhase.HANDLERS);
            shouldDelegate |= runHandlers(ProcessPhase.HANDLER_DONE, instanceContext.getProcessDefinition().getProcessHandlers());

            instanceContext.setPhase(ProcessPhase.POST_LISTENERS);
            shouldDelegate |= runHandlers(ProcessPhase.POST_LISTENERS_DONE, instanceContext.getProcessDefinition().getPostProcessListeners());

            if ( shouldDelegate ) {
                throw new ProcessCancelException("Process result triggered a delegation");
            }
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
            throw new ProcessExecutionExitException("Previous state [" + previousState +
                    "] does not equal current state [" + newState + "]", STATE_CHANGED);
        }
    }

    public ProcessRecord getProcessRecord() {
        record.setPhase(instanceContext.getPhase());
        record.setProcessLog(processLog);
        record.setExitReason(finalReason);

        if ( finalReason == null ) {
            record.setRunningProcessServerId(EngineContext.getProcessServerId());
        } else {
            record.setRunningProcessServerId(null);
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
        log.info("Changing state [{}->{}] on [{}:{}]", previousState, newState, record.getResourceType(),
                record.getResourceId());
        execution.getTransitions().add(new ProcessStateTransition(previousState, newState, "transitioning", now()));
        publishChanged();
    }

    protected void setDone() {
        ProcessState state = instanceContext.getState();

        String previousState = state.getState();
        String newState = state.setDone();
        log.info("Changing state [{}->{}] on [{}:{}]", previousState, newState, record.getResourceType(),
                record.getResourceId());
        execution.getTransitions().add(new ProcessStateTransition(previousState, newState, "done", now()));
        publishChanged();
    }

    protected void publishChanged() {
        context.getEventService().publish(EventVO
                .newEvent(FrameworkEvents.STATE_CHANGE)
                .withResourceType(record.getResourceType())
                .withResourceId(record.getResourceId()));
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

    @Override
    public String toString() {
        try {
            return "Process [" + instanceContext.getProcessDefinition().getName() +
                    "] resource [" + instanceContext.getState().getResourceId() + "]";
        } catch ( NullPointerException e ) {
            return super.toString();
        }
    }
}
