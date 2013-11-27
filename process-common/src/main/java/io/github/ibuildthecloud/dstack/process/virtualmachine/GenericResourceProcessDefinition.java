package io.github.ibuildthecloud.dstack.process.virtualmachine;

import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import io.github.ibuildthecloud.dstack.engine.process.AbstractProcessDefinition;
import io.github.ibuildthecloud.dstack.engine.process.LaunchConfiguration;
import io.github.ibuildthecloud.dstack.engine.process.ProcessState;
import io.github.ibuildthecloud.dstack.engine.process.impl.ResourceStatesDefinition;
import io.github.ibuildthecloud.dstack.object.ObjectManager;

public class GenericResourceProcessDefinition extends AbstractProcessDefinition {

    String name;
    ResourceStatesDefinition statesDefintion = new ResourceStatesDefinition();
    ObjectManager objectManager;

    @Override
    public ProcessState constructProcessState(LaunchConfiguration config) {
        return new GenericResourceProcessState(statesDefintion, config, objectManager);
    }

    @Override
    public String getName() {
        return name;
    }

    @Inject
    public void setName(String name) {
        this.name = name;
    }

    public Set<String> getStartStates() {
        return statesDefintion.getStartStates();
    }

    public void setStartStates(Set<String> startStates) {
        statesDefintion.setStartStates(startStates);
    }

    public Set<String> getTransitioningStates() {
        return statesDefintion.getTransitioningStates();
    }

    public void setTransitioningStates(Set<String> transitioningStates) {
        statesDefintion.setTransitioningStates(transitioningStates);
    }

    public Set<String> getDoneStates() {
        return statesDefintion.getDoneStates();
    }

    public void setDoneStates(Set<String> doneStates) {
        statesDefintion.setDoneStates(doneStates);
    }

    public Map<String, String> getTransitioningStatesMap() {
        return statesDefintion.getTransitioningStatesMap();
    }

    public void setTransitioningStatesMap(Map<String, String> transitioningStatesMap) {
        statesDefintion.setTransitioningStatesMap(transitioningStatesMap);
    }

    public Map<String, String> getDoneStatesMap() {
        return statesDefintion.getDoneStatesMap();
    }

    public void setDoneStatesMap(Map<String, String> doneStatesMap) {
        statesDefintion.setDoneStatesMap(doneStatesMap);
    }

    public String getStateField() {
        return statesDefintion.getStateField();
    }

    public void setStateField(String stateField) {
        statesDefintion.setStateField(stateField);
    }

    public ResourceStatesDefinition getStatesDefintion() {
        return statesDefintion;
    }

    public void setStatesDefintion(ResourceStatesDefinition statesDefintion) {
        this.statesDefintion = statesDefintion;
    }

    public ObjectManager getObjectManager() {
        return objectManager;
    }

    @Inject
    public void setObjectManager(ObjectManager objectManager) {
        this.objectManager = objectManager;
    }

}
