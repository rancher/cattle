package io.cattle.platform.engine.process;

public class ProcessStateTransition {
    String oldState;
    String newState;
    String newProcessState;
    long time;

    public ProcessStateTransition() {
    }

    public ProcessStateTransition(String oldState, String newState, String newProcessState, long time) {
        super();
        this.oldState = oldState;
        this.newState = newState;
        this.newProcessState = newProcessState;
        this.time = time;
    }

    public String getOldState() {
        return oldState;
    }

    public void setOldState(String oldState) {
        this.oldState = oldState;
    }

    public String getNewState() {
        return newState;
    }

    public void setNewState(String newState) {
        this.newState = newState;
    }

    public String getNewProcessState() {
        return newProcessState;
    }

    public void setNewProcessState(String newProcessState) {
        this.newProcessState = newProcessState;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }
}
