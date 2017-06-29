package io.cattle.platform.engine.process.log;

public class ProcessLogicExecutionLog extends AbstractParentLog implements ParentLog {

    String name;
    long startTime;
    Long stopTime;
    ExceptionLog exception;
    String chainProcessName;

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public Long getStopTime() {
        return stopTime;
    }

    public void setStopTime(Long stopTime) {
        this.stopTime = stopTime;
    }

    public ExceptionLog getException() {
        return exception;
    }

    public void setException(ExceptionLog exception) {
        this.exception = exception;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getChainProcessName() {
        return chainProcessName;
    }

    public void setChainProcessName(String chainProcessName) {
        this.chainProcessName = chainProcessName;
    }

}
