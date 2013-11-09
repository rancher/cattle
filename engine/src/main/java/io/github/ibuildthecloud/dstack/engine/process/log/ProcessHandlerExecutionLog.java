package io.github.ibuildthecloud.dstack.engine.process.log;

public class ProcessHandlerExecutionLog {

    String name;
    long startTime;
    long stopTime;
    ExceptionLog exception;

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getStopTime() {
        return stopTime;
    }

    public void setStopTime(long stopTime) {
        this.stopTime = stopTime;
    }

    public ExceptionLog getException() {
        return exception;
    }

    public void setException(ExceptionLog exception) {
        this.exception = exception;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
