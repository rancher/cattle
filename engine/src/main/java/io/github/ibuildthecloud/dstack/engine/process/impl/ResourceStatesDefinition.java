package io.github.ibuildthecloud.dstack.engine.process.impl;

import io.github.ibuildthecloud.dstack.engine.process.StateTransition;
import io.github.ibuildthecloud.dstack.engine.process.StateTransition.Style;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ResourceStatesDefinition {

    public static final String DEFAULT_STATE_FIELD = "state";

    String stateField = DEFAULT_STATE_FIELD;
    Set<String> startStates = new HashSet<String>();
    Set<String> requiredFields = new HashSet<String>();
    Map<String,String> transitioningStatesMap = new HashMap<String, String>();
    Map<String,String> doneStatesMap = new HashMap<String, String>();

    public List<StateTransition> getStateTransitions() {
        List<StateTransition> result = new ArrayList<StateTransition>();

        for ( Map.Entry<String, String> entry : transitioningStatesMap.entrySet() ) {
            String key = entry.getKey();
            String value = entry.getValue();

            if ( key == null ) {
                for ( String start : startStates ) {
                    result.add(new StateTransition(start, value, stateField, Style.TRANSITIONING));
                }
            } else {
                result.add(new StateTransition(key, value, stateField, Style.TRANSITIONING));
            }
        }

        for ( Map.Entry<String, String> entry : doneStatesMap.entrySet() ) {
            String key = entry.getKey();
            String value = entry.getValue();

            if ( key == null ) {
                for ( String start : transitioningStatesMap.values()) {
                    result.add(new StateTransition(start, value, stateField, Style.DONE));
                }
            } else {
                result.add(new StateTransition(key, value, stateField, Style.DONE));
            }
        }

        return result;
    }

    public String getTransitioningState(String currentState) {
        String newState = transitioningStatesMap.get(currentState);
        if ( newState == null && transitioningStatesMap.size() == 1) {
            return transitioningStatesMap.get(null);
        }

        if ( newState == null )
            throw new IllegalStateException("Failed to find state to transition from [" + currentState +
                    "] to \"transitioning\"");

        return null;
    }

    public String getDoneState(String currentState) {
        String newState = doneStatesMap.get(currentState);
        if ( newState == null && doneStatesMap.size() == 1) {
            return doneStatesMap.get(null);
        }

        if ( newState == null )
            throw new IllegalStateException("Failed to find state to transition from [" + currentState +
                    "] to \"done\"");

        return null;
    }

    public boolean isDone(String currentState) {
        return doneStatesMap.containsValue(currentState);
    }

    public boolean isTransitioning(String currentState) {
        return transitioningStatesMap.containsValue(currentState) ||
                doneStatesMap.containsKey(currentState);
    }

    public boolean isStart(String currentState) {
        return startStates.contains(currentState);
    }

    public boolean isValidState(String currentState) {
        return isStart(currentState) || isTransitioning(currentState) || isDone(currentState);
    }

    public Set<String> getStartStates() {
        return startStates;
    }

    public void setStartStates(Set<String> startStates) {
        this.startStates = startStates;
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