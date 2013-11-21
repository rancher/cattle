package io.github.ibuildthecloud.dstack.engine.process.impl;

import static io.github.ibuildthecloud.dstack.engine.process.ExitReason.*;
import static io.github.ibuildthecloud.dstack.util.time.TimeUtils.*;
import io.github.ibuildthecloud.dstack.engine.context.EngineContext;
import io.github.ibuildthecloud.dstack.engine.handler.ProcessHandler;
import io.github.ibuildthecloud.dstack.engine.handler.ProcessListener;
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
import io.github.ibuildthecloud.dstack.engine.process.log.ProcessHandlerExecutionLog;
import io.github.ibuildthecloud.dstack.engine.process.log.ProcessLog;
import io.github.ibuildthecloud.dstack.engine.repository.ProcessManager;
import io.github.ibuildthecloud.dstack.engine.repository.impl.ProcessRecord;
import io.github.ibuildthecloud.dstack.lock.LockCallbackNoReturn;
import io.github.ibuildthecloud.dstack.lock.LockManager;
import io.github.ibuildthecloud.dstack.lock.definition.LockDefinition;
import io.github.ibuildthecloud.dstack.lock.exception.FailedToAcquireLockException;
import io.github.ibuildthecloud.dstack.lock.util.LockUtils;

import java.util.Date;
import java.util.List;

public class DefaultProcessInstanceImpl implements ProcessInstance {

    ProcessManager repository;
    LockManager lockManager;

    ProcessDefinition processDefinition;
    ProcessState state;

    Long id;
    ProcessLog log;
    ProcessExecutionLog execution;
    LockDefinition processLock;
    ProcessPhase phase;
    ExitReason finalReason;
    ProcessRecord record;
//    String previousState;
    volatile boolean inLogic = false;

    public DefaultProcessInstanceImpl(ProcessManager repository, LockManager lockManager, ProcessRecord record,
            ProcessDefinition processDefinition, ProcessState state) {
        super();
        this.id = record.getId();
        this.repository = repository;
        this.lockManager = lockManager;
        this.processDefinition = processDefinition;
        this.state = state;
        this.log = record.getProcessLog();
        this.phase = record.getPhase();
        this.record = record;
    }

    @Override
    public ExitReason execute() {
        EngineContext engineContext = EngineContext.getEngineContext();
        try {
            if ( log == null ) {
                ParentLog parentLog = engineContext.peekLog();
                if ( parentLog == null ) {
                    log = new ProcessLog();
                } else {
                    log = parentLog.newChildLog();
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
            return exit(e.getExitReason());
        } finally {
            repository.persistState(this);
        }
    }

    protected void executeInternal() {
        execution = log.newExecution();
        EngineContext.getEngineContext().pushLog(execution);

        try {
            preRunStateCheck();

            acquireLockAndRun();
        } catch ( Throwable t) {
            execution.setException(new ExceptionLog(t));
            throw new ProcessExecutionExitException(UNKNOWN_EXCEPTION);
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

        runLogic();

        setDone();
    }

    protected void runListeners(List<ProcessListener> listeners, ProcessPhase listenerPhase, 
            ExitReason exceptionReason) {
        if ( phase.ordinal() >= listenerPhase.ordinal() )
            return;

        boolean ran = false;
        EngineContext context = EngineContext.getEngineContext();
        for ( ProcessListener listener : listeners ) {
            ProcessHandlerExecutionLog processExecution = execution.newProcessHandlerExecution(listener);
            context.pushLog(processExecution);
            try {
                ran = true;
                listener.handle(state, this);
            } catch ( Throwable t ) {
                processExecution.setException(new ExceptionLog(t));
                throw new ProcessExecutionExitException(exceptionReason);
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

    protected void runHanlders() {
        boolean ran = false;

        if ( phase.ordinal() < ProcessPhase.HANDLER_DONE.ordinal() ) {
            EngineContext context = EngineContext.getEngineContext();
            for ( ProcessHandler handler : processDefinition.getProcessHandlers() ) {
                ProcessHandlerExecutionLog processExecution = execution.newProcessHandlerExecution(handler);
                context.pushLog(processExecution);
                try {
                    ran = true;
                    handler.handle(state, this);
                } catch ( Throwable t ) {
                    processExecution.setException(new ExceptionLog(t));
                    throw new ProcessExecutionExitException(HANDLER_EXCEPTION);
                } finally {
                    processExecution.setStopTime(now());
                    context.popLog();
                }
            }

            phase = ProcessPhase.HANDLER_DONE;
        }

        if ( ran ) {
            assertState();
        }
    }

    protected void runLogic() {
        inLogic = true;
        try {
            runListeners(processDefinition.getPreProcessListeners(), ProcessPhase.PRE_LISTENERS_DONE,
                    ExitReason.PRE_HANDLER_EXCEPTION);

            runHanlders();

            runListeners(processDefinition.getPreProcessListeners(), ProcessPhase.POST_LISTENERS_DONE,
                    ExitReason.POST_HANDLER_EXCEPTION);
        } finally {
            inLogic = false;
        }
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
        record.setProcessLog(log);
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
