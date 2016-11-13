package io.cattle.platform.engine.process.log;

import static io.cattle.platform.util.time.TimeUtils.*;

import io.cattle.platform.engine.process.ExitReason;
import io.cattle.platform.engine.process.ProcessStateTransition;
import io.cattle.platform.util.type.Named;

import java.util.ArrayList;
import java.util.List;

public class ProcessExecutionLog extends AbstractParentLog implements ParentLog {

    String id = io.cattle.platform.util.resource.UUID.randomUUID().toString();
    long startTime;
    String processName;
    Long stopTime;
    String processingServerId;
    String resourceType;
    String resourceId;
    Long processId;
    List<ProcessStateTransition> transitions = new ArrayList<ProcessStateTransition>();
    List<ProcessLogicExecutionLog> processHandlerExecutions = new ArrayList<ProcessLogicExecutionLog>();
    ExceptionLog exception;

    ExitReason exitReason;

    public ExitReason exit(ExitReason reason) {
        this.stopTime = System.currentTimeMillis();
        this.exitReason = reason;
        return reason;
    }

    public ProcessLogicExecutionLog newProcessLogicExecution(Named handler) {
        if (handler == null) {
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

    public String getProcessingServerId() {
        return processingServerId;
    }

    public void setProcessingServerId(String processingServerId) {
        this.processingServerId = processingServerId;
    }

    public List<ProcessStateTransition> getTransitions() {
        return transitions;
    }

    public void setTransitions(List<ProcessStateTransition> transitions) {
        this.transitions = transitions;
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