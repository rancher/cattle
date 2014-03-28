package io.cattle.platform.engine.process.log;

import static io.cattle.platform.util.time.TimeUtils.*;
import io.cattle.platform.engine.process.ExitReason;
import io.cattle.platform.engine.process.ProcessStateTransition;
import io.cattle.platform.util.type.Named;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ProcessExecutionLog extends AbstractParentLog implements ParentLog {

    String id = UUID.randomUUID().toString();
    long startTime;
    String processName;
    Long stopTime;
    String processLock;
    Long lockAcquireStart;
    Long lockAcquired;
    Long lockAcquireEnd;
    Long lockAcquireFailed;
    String failedToAcquireLock;
    Long lockHoldTime;
    String processingServerId;
    String resourceType;
    String resourceId;
    Long processId;
    List<ProcessStateTransition> transitions = new ArrayList<ProcessStateTransition>();
    List<ProcessLogicExecutionLog> processHandlerExecutions = new ArrayList<ProcessLogicExecutionLog>();
    ExceptionLog exception;

//    List<ProcessLogicExecutionLog> handlerExecutions = new ArrayList<ProcessLogicExecutionLog>();
//    List<ProcessExecutionLog> executions = new ArrayList<ProcessExecutionLog>();

    ExitReason exitReason;

    public ExitReason exit(ExitReason reason) {
        this.stopTime = System.currentTimeMillis();
        this.exitReason = reason;
        return reason;
    }

    public void close() {
        if ( processLock != null && lockAcquired != null && lockAcquireEnd != null ) {
            lockHoldTime = lockAcquireEnd - lockAcquired;
        }
    }

    public ProcessLogicExecutionLog newProcessLogicExecution(Named handler) {
        if ( handler == null ) {
            return new ProcessLogicExecutionLog();
        }
        ProcessLogicExecutionLog execution = new ProcessLogicExecutionLog();
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

    public String getProcessingServerId() {
        return processingServerId;
    }

    public void setProcessingServerId(String processingServerId) {
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

    public Long getLockAcquireFailed() {
        return lockAcquireFailed;
    }

    public void setLockAcquireFailed(Long lockAcquireFailed) {
        this.lockAcquireFailed = lockAcquireFailed;
    }

    public List<ProcessLogicExecutionLog> getProcessHandlerExecutions() {
        return processHandlerExecutions;
    }

    public void setProcessHandlerExecutions(List<ProcessLogicExecutionLog> processHandlerExecutions) {
        this.processHandlerExecutions = processHandlerExecutions;
    }

    public ExceptionLog getException() {
        return exception;
    }

    public void setException(ExceptionLog exception) {
        this.exception = exception;
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

    public String getProcessName() {
        return processName;
    }

    public void setProcessName(String processName) {
        this.processName = processName;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    @Override
    public String getName() {
        return processName;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Long getProcessId() {
        return processId;
    }

    public void setProcessId(Long processId) {
        this.processId = processId;
    }

}