package io.github.ibuildthecloud.dstack.engine.process.impl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ResourceStatesDefinition {

    String stateField = "state";
    Set<String> startStates = new HashSet<String>();
    Set<String> transitioningStates = new HashSet<String>();
    Set<String> doneStates = new HashSet<String>();
    Set<String> requiredFields = new HashSet<String>();
    Map<String,String> transitioningStatesMap = new HashMap<String, String>();
    Map<String,String> doneStatesMap = new HashMap<String, String>();

    public String getTransitioningState(String currentState) {
        String newState = transitioningStatesMap.get(currentState);
        if ( newState == null && transitioningStates.size() == 1) {
            return transitioningStates.iterator().next();
        }

        if ( newState == null )
            throw new IllegalStateException("Failed to find state to transition from [" + currentState + 
                    "] to \"transitioning\"");

        return null;
    }

    public String getDoneState(String currentState) {
        String newState = doneStatesMap.get(currentState);
        if ( newState == null && doneStates.size() == 1) {
            return doneStates.iterator().next();
        }

        if ( newState == null )
            throw new IllegalStateException("Failed to find state to transition from [" + currentState + 
                    "] to \"done\"");

        return null;
    }

    public boolean isDone(String currentState) {
        return doneStates.contains(currentState);        
    }

    public boolean isTransitioning(String currentState) {
        return transitioningStates.contains(currentState);        
    }
    
    public boolean isStart(String currentState) {
        return startStates.contains(currentState);        
    }

    public boolean isValidState(String currentState) {
        return isStart(currentState) || isTransitioning(currentState) || isDone(currentState);
    }

    public void init() {
        if ( doneStates.size() > 0 && doneStatesMap.size() == 0) {
            throw new IllegalStateException("If there are more than one done state you must set doneStatesMap");
        }

        if ( transitioningStates.size() > 0 && transitioningStatesMap.size() == 0) {
            throw new IllegalStateException("If there are more than one transitioning states you must set transitioningStatesMap");
        }
    }

    public Set<String> getStartStates() {
        return startStates;
    }

    public void setStartStates(Set<String> startStates) {
        this.startStates = startStates;
    }

    public Set<String> getTransitioningStates() {
        return transitioningStates;
    }

    public void setTransitioningStates(Set<String> transitioningStates) {
        this.transitioningStates = transitioningStates;
    }

    public Set<String> getDoneStates() {
        return doneStates;
    }

    public void setDoneStates(Set<String> doneStates) {
        this.doneStates = doneStates;
    }

    public Map<String, String> getTransitioningStatesMap() {
        return transitioningStatesMap;
    }

    public void setTransitioningStatesMap(Map<String, String> transitioningStatesMap) {
        this.transitioningStatesMap = transitioningStatesMap;
    }

    public Map<String, String> getDoneStatesMap() {
        return doneStatesMap;
    }

    public void setDoneStatesMap(Map<String, String> doneStatesMap) {
        this.doneStatesMap = doneStatesMap;
    }

    public String getStateField() {
        return stateField;
    }

    public void setStateField(String stateField) {
        this.stateField = stateField;
    }

    public Set<String> getRequiredFields() {
        return requiredFields;
    }

    public void setRequiredFields(Set<String> requiredFields) {
        this.requiredFields = requiredFields;
    }

}