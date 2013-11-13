package io.github.ibuildthecloud.dstack.engine.repository.impl;

import io.github.ibuildthecloud.dstack.engine.context.EngineContext;
import io.github.ibuildthecloud.dstack.engine.process.ExitReason;
import io.github.ibuildthecloud.dstack.engine.process.LaunchConfiguration;
import io.github.ibuildthecloud.dstack.engine.process.ProcessPhase;
import io.github.ibuildthecloud.dstack.engine.process.ProcessResult;
import io.github.ibuildthecloud.dstack.engine.process.log.ProcessLog;

import java.util.Date;

public class ProcessRecord extends LaunchConfiguration {
    Long id;
    Date startTime;
    Date endTime;
    ProcessLog processLog;
    ProcessResult result;
    ExitReason exitReason;
    ProcessPhase phase = ProcessPhase.REQUESTED;
    Long startProcessServerId;
    Long runningProcessServerId;

    public ProcessRecord() {
    }

    public ProcessRecord(LaunchConfiguration config, Long id, Long startProcessServerId) {
        super(config);
        this.id = id;
        this.startProcessServerId = startProcessServerId;
        this.startTime = new Date();

        if ( this.startProcessServerId == null ) {
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

    public Long getStartProcessServerId() {
        return startProcessServerId;
    }

    public void setStartProcessServerId(Long startProcessServerId) {
        this.startProcessServerId = startProcessServerId;
    }

    public Long getRunningProcessServerId() {
        return runningProcessServerId;
    }

    public void setRunningProcessServerId(Long runningProcessServerId) {
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
}
