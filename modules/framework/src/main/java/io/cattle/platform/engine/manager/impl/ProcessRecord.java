package io.cattle.platform.engine.manager.impl;

import io.cattle.platform.engine.context.EngineContext;
import io.cattle.platform.engine.process.ExitReason;
import io.cattle.platform.engine.process.LaunchConfiguration;
import io.cattle.platform.engine.process.ProcessResult;
import io.cattle.platform.engine.process.log.ProcessLog;

import java.util.Date;

public class ProcessRecord extends LaunchConfiguration {
    protected Long id;
    protected Date startTime;
    protected Date endTime;
    protected ProcessLog processLog;
    protected ProcessResult result;
    protected ExitReason exitReason;
    protected String startProcessServerId;
    protected String runningProcessServerId;
    protected long executionCount = 0;

    public ProcessRecord() {
    }

    public ProcessRecord(LaunchConfiguration config, Long id, String startProcessServerId) {
        super(config);
        this.id = id;
        this.startProcessServerId = startProcessServerId;
        this.startTime = new Date();

        if (this.startProcessServerId == null) {
            this.startProcessServerId = EngineContext.getProcessServerId();
        }
    }

    public Date getStartTime() {
        return startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public ProcessLog getProcessLog() {
        return processLog;
    }

    public String getStartProcessServerId() {
        return startProcessServerId;
    }

    public String getRunningProcessServerId() {
        return runningProcessServerId;
    }

    public Long getId() {
        return id;
    }

    public ExitReason getExitReason() {
        return exitReason;
    }

    public ProcessResult getResult() {
        return result;
    }

    public long getExecutionCount() {
        return executionCount;
    }

    public void setExitReason(ExitReason exitReason) {
        this.exitReason = exitReason;
    }

    public void setExecutionCount(long executionCount) {
        this.executionCount = executionCount;
    }

    public void setRunningProcessServerId(String runningProcessServerId) {
        this.runningProcessServerId = runningProcessServerId;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public void setProcessLog(ProcessLog processLog) {
        this.processLog = processLog;
    }

    public void setResult(ProcessResult result) {
        this.result = result;
    }

}
