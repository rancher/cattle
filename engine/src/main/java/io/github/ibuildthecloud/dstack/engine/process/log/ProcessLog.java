package io.github.ibuildthecloud.dstack.engine.process.log;

import io.github.ibuildthecloud.dstack.engine.context.EngineContext;

import java.util.ArrayList;
import java.util.List;

public class ProcessLog {

    List<ProcessHandlerExecutionLog> handlerExecutions = new ArrayList<ProcessHandlerExecutionLog>();
    List<ProcessExecutionLog> executions = new ArrayList<ProcessExecutionLog>();
    List<ProcessLog> children = new ArrayList<ProcessLog>();

    public ProcessExecutionLog newExecution() {
        ProcessExecutionLog execution = new ProcessExecutionLog();
        execution.setStartTime(System.currentTimeMillis());
        execution.setProcessingServerId(EngineContext.getEngineContext().getProcessingServerId());
        executions.add(execution);
        return execution;
    }

    public ProcessLog newChildLog() {
        ProcessLog log = new ProcessLog();
        children.add(log);
        return log;
    }

    public List<ProcessExecutionLog> getExecutions() {
        return executions;
    }

    public void setExecutions(List<ProcessExecutionLog> executions) {
        this.executions = executions;
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

}
