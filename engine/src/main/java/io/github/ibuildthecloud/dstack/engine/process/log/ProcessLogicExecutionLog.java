package io.github.ibuildthecloud.dstack.engine.process.log;

import java.util.Map;
import java.util.Set;

public class ProcessLogicExecutionLog extends AbstractParentLog implements ParentLog {

    String name;
    long startTime;
    Long stopTime;
    ExceptionLog exception;
    boolean shouldContinue;
    Map<String,Object> resultData;
    Map<String,Object> resourceValueBefore;
    Map<String,Object> resourceValueAfter;
    Set<String> missingRequiredFields;

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

    public Map<String, Object> getResultData() {
        return resultData;
    }

    public void setResultData(Map<String, Object> resultData) {
        this.resultData = resultData;
    }

    public Set<String> getMissingRequiredFields() {
        return missingRequiredFields;
    }

    public void setMissingRequiredFields(Set<String> missingRequiredFields) {
        this.missingRequiredFields = missingRequiredFields;
    }

    public Map<String, Object> getResourceValueAfter() {
        return resourceValueAfter;
    }

    public void setResourceValueAfter(Map<String, Object> resourceValueAfter) {
        this.resourceValueAfter = resourceValueAfter;
    }

    public Map<String, Object> getResourceValueBefore() {
        return resourceValueBefore;
    }

    public void setResourceValueBefore(Map<String, Object> resourceValueBefore) {
        this.resourceValueBefore = resourceValueBefore;
    }

}
