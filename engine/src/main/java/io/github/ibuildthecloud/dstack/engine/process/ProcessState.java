package io.github.ibuildthecloud.dstack.engine.process;

import java.util.Map;

import io.github.ibuildthecloud.dstack.lock.definition.LockDefinition;

public interface ProcessState {

    Object getResource();

    LockDefinition getProcessLock();

    String getState();

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
