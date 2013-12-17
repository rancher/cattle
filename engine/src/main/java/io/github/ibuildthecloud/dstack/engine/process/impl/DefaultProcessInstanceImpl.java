package io.github.ibuildthecloud.dstack.engine.process.impl;

import static io.github.ibuildthecloud.dstack.engine.process.ExitReason.*;
import static io.github.ibuildthecloud.dstack.util.time.TimeUtils.*;
import io.github.ibuildthecloud.dstack.deferred.util.DeferredUtils;
import io.github.ibuildthecloud.dstack.engine.context.EngineContext;
import io.github.ibuildthecloud.dstack.engine.eventing.EngineEvents;
import io.github.ibuildthecloud.dstack.engine.handler.HandlerResult;
import io.github.ibuildthecloud.dstack.engine.handler.ProcessHandler;
import io.github.ibuildthecloud.dstack.engine.handler.ProcessListener;
import io.github.ibuildthecloud.dstack.engine.idempotent.Idempotent;
import io.github.ibuildthecloud.dstack.engine.idempotent.IdempotentExecution;
import io.github.ibuildthecloud.dstack.engine.manager.ProcessManager;
import io.github.ibuildthecloud.dstack.engine.manager.impl.ProcessRecord;
import io.github.ibuildthecloud.dstack.engine.process.ExitReason;
import io.github.ibuildthecloud.dstack.engine.process.ProcessDefinition;
import io.github.ibuildthecloud.dstack.engine.process.ProcessExecutionExitException;
import io.github.ibuildthecloud.dstack.engine.process.ProcessInstance;
import io.github.ibuildthecloud.dstack.engine.process.ProcessPhase;
import io.github.ibuildthecloud.dstack.engine.process.ProcessState;
import io.github.ibuildthecloud.dstack.engine.process.ProcessStateTransition;
import io.github.ibuildthecloud.dstack.engine.process.log.ExceptionLog;
import io.github.ibuildthecloud.dstack.engine.process.log.ParentLog;
import io.github.ibuildthecloud.dstack.engine.process.log.ProcessExecutionLog;
import io.github.ibuildthecloud.dstack.engine.process.log.ProcessLog;
import io.github.ibuildthecloud.dstack.engine.process.log.ProcessLogicExecutionLog;
import io.github.ibuildthecloud.dstack.eventing.EventService;
import io.github.ibuildthecloud.dstack.eventing.model.Event;
import io.github.ibuildthecloud.dstack.eventing.util.EventUtils;
import io.github.ibuildthecloud.dstack.lock.LockCallbackNoReturn;
import io.github.ibuildthecloud.dstack.lock.LockManager;
import io.github.ibuildthecloud.dstack.lock.definition.LockDefinition;
import io.github.ibuildthecloud.dstack.lock.exception.FailedToAcquireLockException;
import io.github.ibuildthecloud.dstack.lock.util.LockUtils;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultProcessInstanceImpl implements ProcessInstance {

    private static final Logger log = LoggerFactory.getLogger(DefaultProcessInstanceImpl.class);

    ProcessManager repository;
    LockManager lockManager;

    ProcessDefinition processDefinition;
    ProcessState state;

    Long id;
    ProcessLog processLog;
    EventService eventService;
    ProcessExecutionLog execution;
    LockDefinition processLock;
    ProcessPhase phase;
    ExitReason finalReason;
    ProcessRecord record;
    volatile boolean inLogic = false;
    boolean executed = false;
    boolean schedule = false;

    public DefaultProcessInstanceImpl(ProcessManager repository, LockManager lockManager, EventService eventService,
            ProcessRecord record, ProcessDefinition processDefinition, ProcessState state) {
        super();
        this.id = record.getId();
        this.repository = repository;
        this.lockManager = lockManager;
        this.processDefinition = processDefinition;
        this.state = state;
        this.processLog = record.getProcessLog();
        this.phase = record.getPhase();
        this.record = record;
        this.eventService = eventService;
    }

    @Override
    public void schedule() {
        schedule = true;
        try {
            execute();
        } catch ( ProcessExecutionExitException e ) {
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

        EngineContext engineContext = EngineContext.getEngineContext();
        try {
            if ( processLog == null ) {
                ParentLog parentLog = engineContext.peekLog();
                if ( parentLog == null ) {
                    processLog = new ProcessLog();
                } else {
                    processLog = parentLog.newChildLog();
                }
            }

            lockManager.lock(new ProcessLock(this), new LockCallbackNoReturn() {
                @Override
                public void doWithLockNoResult() {
                    executeInternal();
                }
            });

            return exit(ExitReason.ACTIVE);
        } catch ( ProcessExecutionExitException e ) {
            if ( e.getCause() != null ) {
                log.error("Exiting with code [{}]", e.getExitReason(), e.getCause());
            }
            exit(e.getExitReason());
            throw e;
        } finally {
            repository.persistState(this);
        }
    }

    protected void lookForCrashes() {
        for ( ProcessExecutionLog exec : processLog.getExecutions() ) {
            if ( exec.getExitReason() == null ) {
                exec.setExitReason(SERVER_TERMINATED);
            }
        }
    }

    protected void executeInternal() {
        log.info("Running process [{}] [{}] for [{}:{}]", id,
                record.getProcessName(), record.getResourceType(), record.getResourceId());

        lookForCrashes();

        execution = processLog.newExecution();
        EngineContext.getEngineContext().pushLog(execution);

        try {
            preRunStateCheck();

            acquireLockAndRun();
        } catch ( ProcessExecutionExitException e ) {
            throw e;
        } catch ( Throwable t) {
            execution.setException(new ExceptionLog(t));
            throw new ProcessExecutionExitException(UNKNOWN_EXCEPTION, t);
        } finally {
            execution.close();
            EngineContext.getEngineContext().popLog();
        }
    }

    protected void acquireLockAndRun() {
        startLock();
        try {
            lockManager.lock(processLock, new LockCallbackNoReturn() {
                @Override
                public void doWithLockNoResult() {
                    runWithProcessLock();
                }
            });
        } catch ( FailedToAcquireLockException e ) {
            lockFailed(e.getLockDefition());
        } finally {
            lockAcquireEnd();
        }
    }

    protected void preRunStateCheck() {
        if ( state.isDone() ) {
            throw new ProcessExecutionExitException(ALREADY_ACTIVE);
        }

        if ( state.shouldCancel() ) {
            throw new ProcessExecutionExitException(CANCELED);
        }
    }

    protected void runWithProcessLock() {
        lockAcquired();

        state.reload();

        preRunStateCheck();

        if ( phase == ProcessPhase.REQUESTED ) {
            phase = ProcessPhase.STARTED;
            repository.persistState(this);
        }

        if ( state.isStart() ) {
            setTransitioning();
        }

        if ( schedule ) {
            runScheduled();
        }

        runLogic();

        setDone();
    }

    protected void runScheduled() {
        DeferredUtils.defer(new Runnable() {
            @Override
            public void run() {
                if ( record.getId() != null ) {
                    Event event = EventUtils.newEvent(EngineEvents.PROCESS_EXECUTE, record.getId().toString());
                    eventService.publish(event);
                }

            }
        });
        throw new ProcessExecutionExitException(SCHEDULED);
    }

    protected void runListeners(List<ProcessListener> listeners, ProcessPhase listenerPhase,
            ExitReason exceptionReason) {
        if ( phase.ordinal() >= listenerPhase.ordinal() )
            return;

        boolean ran = false;
        EngineContext context = EngineContext.getEngineContext();
        for ( ProcessListener listener : listeners ) {
            ProcessLogicExecutionLog processExecution = execution.newProcessLogicExecution(listener);
            context.pushLog(processExecution);
            try {
                ran = true;
                listener.handle(state, this);
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

        phase = listenerPhase;
    }

    protected void runHandlers() {
        boolean ran = false;

        if ( phase.ordinal() < ProcessPhase.HANDLER_DONE.ordinal() ) {
            final EngineContext context = EngineContext.getEngineContext();
            for ( final ProcessHandler handler : processDefinition.getProcessHandlers() ) {
                ran = true;
                Boolean shouldContinue = Idempotent.execute(new IdempotentExecution<Boolean>() {
                    @Override
                    public Boolean execute() {
                        ProcessLogicExecutionLog processExecution = execution.newProcessLogicExecution(handler);
                        context.pushLog(processExecution);
                        try {
                            processExecution.setResourceValueBefore(state.convertData(state.getResource()));
                            HandlerResult result = handler.handle(state, DefaultProcessInstanceImpl.this);
                            if ( result == null ) {
                                return true;
                            }

                            Map<String,Object> resultData = state.convertData(result.getData());

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
                                log.error("Missing field [{}] for handler [{}]", missingFields, handler.getName());
                                throw new ProcessExecutionExitException(MISSING_HANDLER_RESULT_FIELDS);
                            }

                            state.applyData(resultData);
                            processExecution.setResourceValueAfter(state.convertData(state.getResource()));

                            return result.shouldContinue();
                        } catch ( RuntimeException e ) {
                            processExecution.setException(new ExceptionLog(e));
                            throw e;
                        } finally {
                            processExecution.setStopTime(now());
                            context.popLog();
                        }
                    }
                });

                if ( ! shouldContinue ) {
                    break;
                }
            }

            phase = ProcessPhase.HANDLER_DONE;
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

    protected void runLogic() {
        inLogic = true;
        try {
            runListeners(processDefinition.getPreProcessListeners(), ProcessPhase.PRE_LISTENERS_DONE,
                    ExitReason.PRE_HANDLER_EXCEPTION);

            runHandlers();

            runListeners(processDefinition.getPreProcessListeners(), ProcessPhase.POST_LISTENERS_DONE,
                    ExitReason.POST_HANDLER_EXCEPTION);
        } finally {
            inLogic = false;
        }

        phase = ProcessPhase.DONE;
    }

    protected void assertState() {
        String previousState = state.getState();
        state.reload();
        String newState = state.getState();
        if ( ! previousState.equals(newState) ) {
            throw new ProcessExecutionExitException(STATE_CHANGED);
        }
    }

    public ProcessRecord getProcessRecord() {
        record.setPhase(phase);
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
        String previousState = state.getState();
        String newState = state.setTransitioning();
        execution.getTransitions().add(new ProcessStateTransition(previousState, newState, "transitioning", now()));
    }

    protected void setDone() {
        String previousState = state.getState();
        String newState = state.setDone();
        execution.getTransitions().add(new ProcessStateTransition(previousState, newState, "done", now()));
    }

    protected void startLock() {
        processLock = state.getProcessLock();
        execution.setLockAcquireStart(now());
        execution.setProcessLock(LockUtils.serializeLock(processLock));
    }

    protected void lockAcquired() {
        execution.setLockAcquired(now());
    }

    protected void lockAcquireEnd() {
        if ( execution.getLockAcquired() != null )
            execution.setLockAcquireEnd(now());
    }

    protected void lockFailed(LockDefinition lockDef) {
        execution.setFailedToAcquireLock(LockUtils.serializeLock(lockDef));
        execution.setLockAcquireFailed(now());
        throw new ProcessExecutionExitException(FAILED_TO_ACQUIRE_LOCK);
    }

    protected ExitReason exit(ExitReason reason) {
        return finalReason = execution.exit(reason);
    }

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public boolean isRunningLogic() {
        return inLogic;
    }

    @Override
    public ExitReason getExitReason() {
        return finalReason;
    }

}
