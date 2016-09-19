package io.cattle.platform.engine.process.log;

import io.cattle.platform.engine.context.EngineContext;

import java.util.ArrayList;
import java.util.List;

public class ProcessLog {

    String uuid = io.cattle.platform.util.resource.UUID.randomUUID().toString();
    List<ProcessExecutionLog> executions = new ArrayList<ProcessExecutionLog>();

    public ProcessExecutionLog newExecution() {
        ProcessExecutionLog execution = new ProcessExecutionLog();
        execution.setStartTime(System.currentTimeMillis());
        execution.setProcessingServerId(EngineContext.getProcessServerId());
        executions.add(execution);
        return execution;
    }

    public List<ProcessExecutionLog> getExecutions() {
        return executions;
    }

    public void setExecutions(List<ProcessExecutionLog> executions) {
        this.executions = executions;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

}
