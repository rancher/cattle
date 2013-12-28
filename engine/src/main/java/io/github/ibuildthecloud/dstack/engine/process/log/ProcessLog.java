package io.github.ibuildthecloud.dstack.engine.process.log;

import io.github.ibuildthecloud.dstack.engine.context.EngineContext;

import java.util.ArrayList;
import java.util.List;

public class ProcessLog {

    List<ProcessExecutionLog> executions = new ArrayList<ProcessExecutionLog>();

    public ProcessExecutionLog newExecution() {
        ProcessExecutionLog execution = new ProcessExecutionLog();
        execution.setStartTime(System.currentTimeMillis());
        execution.setProcessingServerId(EngineContext.getEngineContext().getProcessingServerId());
        executions.add(execution);
        return execution;
    }

    public List<ProcessExecutionLog> getExecutions() {
        return executions;
    }

    public void setExecutions(List<ProcessExecutionLog> executions) {
        this.executions = executions;
    }

}
