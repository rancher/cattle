package io.github.ibuildthecloud.dstack.engine.process.log;

import static io.github.ibuildthecloud.dstack.util.time.TimeUtils.*;
import io.github.ibuildthecloud.dstack.engine.handler.ProcessHandler;
import io.github.ibuildthecloud.dstack.engine.process.ExitReason;
import io.github.ibuildthecloud.dstack.engine.process.ProcessStateTransition;

import java.util.ArrayList;
import java.util.List;

public class ProcessExecutionLog extends AbstractParentLog implements ParentLog {

    long startTime;
    Long stopTime;
    String processLock;
    Long transitionToActivating;
    Long transitionToActive;
    Long lockAcquireStart;
    Long lockAcquired;
    Long lockAcquireEnd;
    Long lockAcquireFailed;
    String failedToAcquireLock;
    Long lockHoldTime;
    Long processingServerId;
    List<ProcessStateTransition> transitions = new ArrayList<ProcessStateTransition>();
    List<ProcessHandlerExecutionLog> processHandlerExecutions = new ArrayList<ProcessHandlerExecutionLog>();
    ExceptionLog exception;

    List<ProcessHandlerExecutionLog> handlerExecutions = new ArrayList<ProcessHandlerExecutionLog>();
    List<ProcessExecutionLog> executions = new ArrayList<ProcessExecutionLog>();

    ExitReason exitReason;

    public ExitReason exit(ExitReason reason) {
        this.stopTime = System.currentTimeMillis();
        this.exitReason = reason;
        return reason;
    }

    public void recordTransition(ProcessStateTransition transition) {
        switch (transition) {
        case ACTIVATING:
            transitionToActivating = System.currentTimeMillis();
            break;
        case ACTIVE:
            transitionToActive = System.currentTimeMillis();
            break;
        default:
            break;
        }

        transitions.add(transition);
    }

    public void close() {
        if ( processLock != null && lockAcquired != null && lockAcquireEnd != null ) {
            lockHoldTime = lockAcquireEnd - lockAcquired;
        }
    }

    public ProcessHandlerExecutionLog newProcessHandlerExecution(ProcessHandler handler) {
        ProcessHandlerExecutionLog execution = new ProcessHandlerExecutionLog();
        execution.setStartTime(now());
        execution.setName(handler.getName());
        processHandlerExecutions.add(execution);
        return execution;
    }

    /* Standard Accessors below */
    public Long getStartTime() {
        return startTime;
    }

    public ExitReason getExitReason() {
        return exitReason;
    }

    public void setExitReason(ExitReason exitReason) {
        this.exitReason = exitReason;
    }

    public Long getStopTime() {
        return stopTime;
    }

    public void setStopTime(Long stopTime) {
        this.stopTime = stopTime;
    }

    public String getProcessLock() {
        return processLock;
    }

    public void setProcessLock(String processLock) {
        this.processLock = processLock;
    }

    public Long getLockAcquireStart() {
        return lockAcquireStart;
    }

    public void setLockAcquireStart(Long lockAcquireStart) {
        this.lockAcquireStart = lockAcquireStart;
    }

    public Long getLockAcquired() {
        return lockAcquired;
    }

    public void setLockAcquired(Long lockAcquired) {
        this.lockAcquired = lockAcquired;
    }

    public Long getLockAcquireEnd() {
        return lockAcquireEnd;
    }

    public void setLockAcquireEnd(Long lockAcquireEnd) {
        this.lockAcquireEnd = lockAcquireEnd;
    }

    public Long getProcessingServerId() {
        return processingServerId;
    }

    public void setProcessingServerId(Long processingServerId) {
        this.processingServerId = processingServerId;
    }

    public Long getLockHoldTime() {
        return lockHoldTime;
    }

    public void setLockHoldTime(Long lockHoldTime) {
        this.lockHoldTime = lockHoldTime;
    }

    public List<ProcessStateTransition> getTransitions() {
        return transitions;
    }

    public void setTransitions(List<ProcessStateTransition> transitions) {
        this.transitions = transitions;
    }

    public Long getTransitionToActivating() {
        return transitionToActivating;
    }

    public void setTransitionToActivating(Long transitionToActivating) {
        this.transitionToActivating = transitionToActivating;
    }

    public Long getTransitionToActive() {
        return transitionToActive;
    }

    public void setTransitionToActive(Long transitionToActive) {
        this.transitionToActive = transitionToActive;
    }

    public Long getLockAcquireFailed() {
        return lockAcquireFailed;
    }

    public void setLockAcquireFailed(Long lockAcquireFailed) {
        this.lockAcquireFailed = lockAcquireFailed;
    }

    public List<ProcessHandlerExecutionLog> getProcessHandlerExecutions() {
        return processHandlerExecutions;
    }

    public void setProcessHandlerExecutions(List<ProcessHandlerExecutionLog> processHandlerExecutions) {
        this.processHandlerExecutions = processHandlerExecutions;
    }

    public ExceptionLog getException() {
        return exception;
    }

    public void setException(ExceptionLog exception) {
        this.exception = exception;
    }

    public List<ProcessHandlerExecutionLog> getHandlerExecutions() {
        return handlerExecutions;
    }

    public void setHandlerExecutions(List<ProcessHandlerExecutionLog> handlerExecutions) {
        this.handlerExecutions = handlerExecutions;
    }

    public List<ProcessLog> getChildren() {
        return children;
    }

    public void setChildren(List<ProcessLog> children) {
        this.children = children;
    }

    public String getFailedToAcquireLock() {
        return failedToAcquireLock;
    }

    public void setFailedToAcquireLock(String failedToAcquireLock) {
        this.failedToAcquireLock = failedToAcquireLock;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public List<ProcessExecutionLog> getExecutions() {
        return executions;
    }

    public void setExecutions(List<ProcessExecutionLog> executions) {
        this.executions = executions;
    }
}
