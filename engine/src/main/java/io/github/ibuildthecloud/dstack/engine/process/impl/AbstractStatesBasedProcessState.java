package io.github.ibuildthecloud.dstack.engine.process.impl;

import io.github.ibuildthecloud.dstack.engine.process.ExitReason;
import io.github.ibuildthecloud.dstack.engine.process.ProcessExecutionExitException;
import io.github.ibuildthecloud.dstack.engine.process.ProcessState;

public abstract class AbstractStatesBasedProcessState implements ProcessState {

    ResourceStatesDefinition statesDefinition;

    public AbstractStatesBasedProcessState(ResourceStatesDefinition statesDef) {
        this.statesDefinition = statesDef;
    }

    @Override
    public String setTransitioning() {
        String oldState = getState();
        String newState = statesDefinition.getTransitioningState(oldState);

        if ( ! setState(oldState, newState) ) {
            throw new ProcessExecutionExitException(ExitReason.STATE_CHANGED);
        }

        return newState;
    }

    protected abstract boolean setState(String oldState, String newState);

    @Override
    public String setDone() {
        String oldState = getState();
        String newState = statesDefinition.getDoneState(oldState);

        if ( ! setState(oldState, newState) ) {
            throw new ProcessExecutionExitException(ExitReason.STATE_CHANGED);
        }

        return newState;
    }

    @Override
    public boolean shouldCancel() {
        return statesDefinition.isValidState(getState());
    }

    @Override
    public boolean isDone() {
        return statesDefinition.isDone(getState());
    }

    @Override
    public boolean isStart() {
        return statesDefinition.isStart(getState());
    }

    @Override
    public boolean isTransitioning() {
        return statesDefinition.isTransitioning(getState());
    }

    public ResourceStatesDefinition getStatesDefinition() {
        return statesDefinition;
    }

}
