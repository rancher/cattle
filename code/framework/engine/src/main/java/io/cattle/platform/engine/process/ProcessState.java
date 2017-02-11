package io.cattle.platform.engine.process;

import io.cattle.platform.engine.manager.impl.ProcessRecord;
import io.cattle.platform.lock.definition.LockDefinition;

import java.util.Map;

public interface ProcessState {

    String getResourceId();

    Object getResource();

    LockDefinition getProcessLock();

    String getState();

    ProcessPhase getPhase();

    void setPhase(ProcessPhase phase);

    Map<String, Object> getData();

    boolean shouldCancel(ProcessRecord record);

    boolean isDone(boolean schedule);

    boolean isStart(ProcessRecord record);

    boolean isTransitioning();

    void reload();

    String setTransitioning();

    String setDone();

    Map<String, Object> convertData(Object data);

    void applyData(Map<String, Object> data);

    void rebuild();

}
