package io.github.ibuildthecloud.dstack.engine.process.impl;

import io.github.ibuildthecloud.dstack.engine.process.ProcessDefinition;
import io.github.ibuildthecloud.dstack.engine.process.ProcessPhase;
import io.github.ibuildthecloud.dstack.engine.process.ProcessState;
import io.github.ibuildthecloud.dstack.lock.definition.LockDefinition;

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
