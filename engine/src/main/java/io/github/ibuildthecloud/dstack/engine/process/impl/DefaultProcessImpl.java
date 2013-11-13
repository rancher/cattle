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
import io.github.ibuildthecloud.dstack.engine.process.log.ParentLog;
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

import java.util.Date;
import java.util.List;

public class DefaultProcessImpl implements ProcessInstance {

    private static final LogicPhase[] PHASES = new LogicPhase[] {
        new LogicPhase(ProcessPhase.PRE_HANDLER_DONE, ExitReason.PRE_HANDLER_EXCEPTION,  ExitReason.PRE_HANDLER_DELAYED),
        new LogicPhase(ProcessPhase.HANDLER_DONE,     ExitReason.HANDLER_EXCEPTION,      ExitReason.HANDLER_DELAYED),
        new LogicPhase(ProcessPhase.PRE_HANDLER_DONE, ExitReason.POST_HANDLER_EXCEPTION, ExitReason.POST_HANDLER_DELAYED)
    };

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
    ProcessRecord record;
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

            return lockManager.lock(new ProcessLock(this), new LockCallback<ExitReason>() {
                @Override
                public ExitReason doWithLock() {
                    return finalReason = executeInternal();
                }
            });
        } finally {
            repository.persistState(this);
        }
    }

    protected ExitReason executeInternal() {
        execution = log.newExecution();
        EngineContext.getEngineContext().pushLog(execution);

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
            EngineContext.getEngineContext().popLog();
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
            return lockFailed(e.getLockDefition());
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
            phase = ProcessPhase.STARTED;
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
            for ( LogicPhase logicPhase : PHASES ) {
                if ( phase.ordinal() < logicPhase.phase.ordinal() ) {
                    reason = runHandlers(processDefinition.getPreProcessHandlers(), logicPhase);
                    if ( reason != null )
                        return reason;

                    phase = logicPhase.phase;
                }
            }

            return reason;
        } finally {
            inLogic = false;
        }
    }

    protected ExitReason runHandlers(List<? extends ProcessHandler> handlers, LogicPhase logicPhase) {
        EngineContext context = EngineContext.getEngineContext();
        for ( ProcessHandler handler : handlers ) {
            ProcessHandlerExecutionLog processExecution = execution.newProcessHandlerExecution(handler);
            context.pushLog(processExecution);
            try {
                handler.handle(this);
            } catch ( Throwable t ) {
                processExecution.setException(new ExceptionLog(t));
                if ( logicPhase.exceptionReason != null )
                    return logicPhase.exceptionReason;
            } finally {
                processExecution.setStopTime(now());
                context.popLog();
            }
        }

        return null;
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

    protected ExitReason lockFailed(LockDefinition lockDef) {
        execution.setFailedToAcquireLock(LockUtils.serializeLock(lockDef));
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

    private static final class LogicPhase {
        ProcessPhase phase;
        ExitReason exceptionReason;
        ExitReason delayReason;

        public LogicPhase(ProcessPhase phase, ExitReason exceptionReason, ExitReason delayReason) {
            super();
            this.phase = phase;
            this.exceptionReason = exceptionReason;
            this.delayReason = delayReason;
        }
    }
}
