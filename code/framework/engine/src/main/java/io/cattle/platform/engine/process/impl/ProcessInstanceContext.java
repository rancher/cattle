package io.cattle.platform.engine.process.impl;

import io.cattle.platform.engine.process.ProcessDefinition;
import io.cattle.platform.engine.process.ProcessPhase;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.lock.definition.LockDefinition;

public class ProcessInstanceContext {

    ProcessDefinition processDefinition;
    ProcessState state;
    LockDefinition processLock;

    public ProcessDefinition getProcessDefinition() {
        return processDefinition;
    }

    public void setProcessDefinition(ProcessDefinition processDefinition) {
        this.processDefinition = processDefinition;
    }

    public ProcessState getState() {
        return state;
    }

    public void setState(ProcessState state) {
        this.state = state;
    }

    public ProcessPhase getPhase() {
        return state.getPhase();
    }

    public void setPhase(ProcessPhase phase) {
        state.setPhase(phase);
    }

    public LockDefinition getProcessLock() {
        return processLock;
    }

    public void setProcessLock(LockDefinition processLock) {
        this.processLock = processLock;
    }

}
