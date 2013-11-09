package io.github.ibuildthecloud.dstack.engine.repository.impl;

import io.github.ibuildthecloud.dstack.engine.process.ProcessPhase;
import io.github.ibuildthecloud.dstack.engine.process.log.ProcessLog;

import java.util.Date;

public class ProcessRecord {
    Long id;
    Date startTime;
    Date endTime;
    String log;
    ProcessPhase phase = ProcessPhase.REQUESTED;
    ProcessLog processLog;
    Long startProcessServerId;
    Long runningProcessServerId;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getLog() {
        return log;
    }

    public void setLog(String log) {
        this.log = log;
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
}
