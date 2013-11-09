package io.github.ibuildthecloud.dstack.engine.process.impl;

import static io.github.ibuildthecloud.dstack.engine.process.ExitReason.*;
import static io.github.ibuildthecloud.dstack.util.time.TimeUtils.*;
import io.github.ibuildthecloud.dstack.engine.context.EngineContext;
import io.github.ibuildthecloud.dstack.engine.handler.ProcessHandler;
import io.github.ibuildthecloud.dstack.engine.process.ExitReason;
import io.github.ibuildthecloud.dstack.engine.process.ProcessDefinition;
import io.github.ibuildthecloud.dstack.engine.process.ProcessInstance;
import io.github.ibuildthecloud.dstack.engine.process.ProcessPhase;
import io.github.ibuildthecloud.dstack.engine.process.ProcessState;
import io.github.ibuildthecloud.dstack.engine.process.ProcessStateTransition;
import io.github.ibuildthecloud.dstack.engine.process.log.ExceptionLog;
import io.github.ibuildthecloud.dstack.engine.process.log.ProcessExecutionLog;
import io.github.ibuildthecloud.dstack.engine.process.log.ProcessHandlerExecutionLog;
import io.github.ibuildthecloud.dstack.engine.process.log.ProcessLog;
import io.github.ibuildthecloud.dstack.engine.repository.ProcessRepository;
import io.github.ibuildthecloud.dstack.engine.repository.impl.ProcessRecord;
import io.github.ibuildthecloud.dstack.lock.LockCallback;
import io.github.ibuildthecloud.dstack.lock.LockManager;
import io.github.ibuildthecloud.dstack.lock.definition.LockDefinition;
import io.github.ibuildthecloud.dstack.lock.exception.FailedToAcquireLockException;
import io.github.ibuildthecloud.dstack.lock.util.LockUtils;

import java.util.List;

public class DefaultProcessImpl implements ProcessInstance {


    ProcessRepository repository;
    LockManager lockManager;

    ProcessDefinition processDefinition;
    ProcessState state;

    Long id;
    ProcessLog log;
    ProcessExecutionLog execution;
    LockDefinition processLock;
    ProcessPhase phase;
    ExitReason finalReason;
    volatile boolean inLogic = false;

    public DefaultProcessImpl(ProcessRepository repository, LockManager lockManager, ProcessRecord record,
            ProcessDefinition processDefinition, ProcessState state) {
        super();
        this.id = record.getId();
        this.repository = repository;
        this.lockManager = lockManager;
        this.processDefinition = processDefinition;
        this.state = state;
        this.log = record.getProcessLog();
        this.phase = record.getPhase();
    }

    @Override
    public ExitReason execute() {
        EngineContext engineContext = EngineContext.getEngineContext();
        try {
            if ( log == null ) {
                ProcessLog parentLog = engineContext.peekProcessLog();
                if ( parentLog == null ) {
                    log = new ProcessLog();
                } else {
                    log = parentLog.newChildLog();
                }
            }

            engineContext.pushProcessLog(log);
            return finalReason = executeInternal();
        } finally {
            engineContext.popProcessLog();
            repository.persistState(this);
        }
    }

    protected ExitReason executeInternal() {
        execution = log.newExecution();

        try {
            ExitReason reason = preRunStateCheck();
            if ( reason != null ) {
                return exit(reason);
            }

            return acquireLockAndRun();
        } catch ( Throwable t) {
            execution.setException(new ExceptionLog(t));
            return exit(UNKNOWN_EXCEPTION);
        } finally {
            execution.close();
        }
    }

    protected ExitReason acquireLockAndRun() {
        startLock();
        try {
            return lockManager.lock(processLock, new LockCallback<ExitReason>() {
                @Override
                public ExitReason doWithLock() {
                    return runWithProcessLock();
                }
            });
        } catch ( FailedToAcquireLockException e ) {
            if ( e.getLockDefition() == processLock ) {
                return lockFailed();
            } else {
                throw e;
            }
        } finally {
            lockAcquireEnd();
        }
    }

    protected ExitReason preRunStateCheck() {
        if ( state.isActive() ) {
            return ALREADY_ACTIVE;
        }

        if ( state.shouldCancel() ) {
            return CANCELED;
        }

        return null;
    }

    protected ExitReason runWithProcessLock() {
        lockAcquired();

        state.reload();

        ExitReason reason = preRunStateCheck();
        if ( reason != null ) {
            return exit(reason);
        }

        if ( phase == ProcessPhase.REQUESTED ) {
            repository.persistState(this);
        }

        if ( state.isInactive() ) {
            setActivating();
        }

        reason = runLogic();
        if ( reason != null )
            return exit(reason);

        setActive();

        return exit(ACTIVE);
    }

    protected ExitReason runLogic() {
        inLogic = true;
        try {
            ExitReason reason = null;
            if ( phase.ordinal() < ProcessPhase.PRE_HANDLER_DONE.ordinal() ) {
                reason = runHandlers(processDefinition.getPreProcessHandlers(), PRE_HANDLER_EXCEPTION, PRE_HANDLER_DELAYED);
                if ( reason != null )
                    return reason;

                phase = ProcessPhase.PRE_HANDLER_DONE;
            }

            if ( phase.ordinal() < ProcessPhase.HANDLER_DONE.ordinal() ) {
                reason = runHandlers(processDefinition.getProcessHandlers(), HANDLER_EXCEPTION, HANDLER_DELAYED);
                if ( reason != null )
                    return reason;
                phase = ProcessPhase.HANDLER_DONE;
            }

            if ( phase.ordinal() < ProcessPhase.POST_HANDLER_DONE.ordinal() ) {
                reason = runHandlers(processDefinition.getProcessHandlers(), POST_HANDLER_EXCEPTION, POST_HANDLER_DELAYED);
                if ( reason != null )
                    return reason;
                phase = ProcessPhase.HANDLER_DONE;
            }

            return reason;
        } finally {
            inLogic = false;
        }
    }

    protected ExitReason runHandlers(List<? extends ProcessHandler> handlers,
            ExitReason exceptionReason, ExitReason delayReason) {
        for ( ProcessHandler handler : handlers ) {
            ProcessHandlerExecutionLog processExecution = execution.newProcessHandlerExecution(handler);
            try {
                handler.handle(this);
            } catch ( Throwable t ) {
                processExecution.setException(new ExceptionLog(t));
                if ( exceptionReason != null )
                    return exceptionReason;
            } finally {
                processExecution.setStopTime(now());
            }
        }

        return null;
    }

    protected void setActivating() {
        state.setActivating();
        execution.recordTransition(ProcessStateTransition.ACTIVATING);
    }

    protected void setActive() {
        state.setActive();
        execution.recordTransition(ProcessStateTransition.ACTIVE);
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

    protected ExitReason lockFailed() {
        execution.setLockAcquireFailed(now());
        return exit(FAILED_TO_ACQUIRE_LOCK);
    }

    protected ExitReason exit(ExitReason reason) {
        return execution.exit(reason);
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
