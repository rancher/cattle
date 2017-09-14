package io.cattle.platform.engine.process.impl;

import io.cattle.platform.engine.process.ProcessDefinition;
import io.cattle.platform.engine.process.ProcessState;

public class ProcessInstanceContext {

    ProcessDefinition processDefinition;
    ProcessState state;
    boolean replay;

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

    public boolean isReplay() {
        return replay;
    }

    public void setReplay(boolean replay) {
        this.replay = replay;
    }

}
