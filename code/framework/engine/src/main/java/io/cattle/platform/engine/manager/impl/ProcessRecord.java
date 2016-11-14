package io.cattle.platform.engine.manager.impl;

import io.cattle.platform.engine.context.EngineContext;
import io.cattle.platform.engine.process.ExitReason;
import io.cattle.platform.engine.process.LaunchConfiguration;
import io.cattle.platform.engine.process.ProcessPhase;
import io.cattle.platform.engine.process.ProcessResult;
import io.cattle.platform.engine.process.log.ProcessLog;

import java.util.Date;

public class ProcessRecord extends LaunchConfiguration {
    Long id;
    Date startTime;
    Date endTime;
    ProcessLog processLog;
    ProcessResult result;
    ExitReason exitReason;
    ProcessPhase phase = ProcessPhase.REQUESTED;
    String startProcessServerId;
    String runningProcessServerId;
    long executionCount = 0;

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

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public ProcessPhase getPhase() {
        return phase;
    }

    public void setPhase(ProcessPhase phase) {
        this.phase = phase;
    }

    public ProcessLog getProcessLog() {
        return processLog;
    }

    public void setProcessLog(ProcessLog processLog) {
        this.processLog = processLog;
    }

    public String getStartProcessServerId() {
        return startProcessServerId;
    }

    public void setStartProcessServerId(String startProcessServerId) {
        this.startProcessServerId = startProcessServerId;
    }

    public String getRunningProcessServerId() {
        return runningProcessServerId;
    }

    public void setRunningProcessServerId(String runningProcessServerId) {
        this.runningProcessServerId = runningProcessServerId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ExitReason getExitReason() {
        return exitReason;
    }

    public void setExitReason(ExitReason exitReason) {
        this.exitReason = exitReason;
    }

    public ProcessResult getResult() {
        return result;
    }

    public void setResult(ProcessResult result) {
        this.result = result;
    }

    public long getExecutionCount() {
        return executionCount;
    }

    public void setExecutionCount(long executionCount) {
        this.executionCount = executionCount;
    }

}
