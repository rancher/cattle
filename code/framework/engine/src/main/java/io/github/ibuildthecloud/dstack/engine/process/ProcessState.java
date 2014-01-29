package io.github.ibuildthecloud.dstack.engine.process;

import io.github.ibuildthecloud.dstack.lock.definition.LockDefinition;

import java.util.Map;

public interface ProcessState {

    String getResourceId();

    Object getResource();

    LockDefinition getProcessLock();

    String getState();

    ProcessPhase getPhase();

    void setPhase(ProcessPhase phase);

    Map<String,Object> getData();

    boolean shouldCancel();

    boolean isDone();

    boolean isStart();

    boolean isTransitioning();

    void reload();

    String setTransitioning();

    String setDone();

    Map<String,Object> convertData(Object data);

    void applyData(Map<String,Object> data);

}
