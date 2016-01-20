package io.cattle.platform.engine.process.log;

public class ProcessLogicExecutionLog extends AbstractParentLog implements ParentLog {

    String name;
    long startTime;
    Long stopTime;
    ExceptionLog exception;
    boolean shouldContinue;
    boolean shouldDelegate;
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

    public boolean isShouldContinue() {
        return shouldContinue;
    }

    public void setShouldContinue(boolean shouldContinue) {
        this.shouldContinue = shouldContinue;
    }

    public boolean isShouldDelegate() {
        return shouldDelegate;
    }

    public void setShouldDelegate(boolean shouldDelegate) {
        this.shouldDelegate = shouldDelegate;
    }

    public String getChainProcessName() {
        return chainProcessName;
    }

    public void setChainProcessName(String chainProcessName) {
        this.chainProcessName = chainProcessName;
    }

}
