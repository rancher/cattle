package io.github.ibuildthecloud.dstack.process.virtualmachine;

import io.github.ibuildthecloud.dstack.engine.process.LaunchConfiguration;
import io.github.ibuildthecloud.dstack.engine.process.ProcessState;
import io.github.ibuildthecloud.dstack.engine.process.ProcessStateFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.lang3.reflect.ConstructorUtils;

public class GenericProcessStateFactory implements ProcessStateFactory {

    String name;
    Class<? extends GenericProcessState<?>> stateClass;

    GenericObjectDataAccess dataAccess;
    String stateField;
    String activatingState;
    String activeState;
    Set<String> cancelStates;
    Set<String> activeStates;
    Set<String> inactiveStates;
    Set<String> activatingStates;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public ProcessState constructProcessState(LaunchConfiguration config) {
        try {
            return ConstructorUtils.invokeConstructor(stateClass, config, this);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(e);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        } catch (InvocationTargetException e) {
            throw new IllegalStateException(e);
        } catch (InstantiationException e) {
            throw new IllegalStateException(e);
        }
    }

    public Class<? extends GenericProcessState<?>> getStateClass() {
        return stateClass;
    }

    public void setStateClass(Class<? extends GenericProcessState<?>> stateClass) {
        this.stateClass = stateClass;
    }

    public GenericObjectDataAccess getDataAccess() {
        return dataAccess;
    }

    @Inject
    public void setDataAccess(GenericObjectDataAccess dataAccess) {
        this.dataAccess = dataAccess;
    }

    public String getStateField() {
        return stateField;
    }

    @Inject
    public void setStateField(String stateField) {
        this.stateField = stateField;
    }

    public String getActivatingState() {
        return activatingState;
    }

    @Inject
    public void setActivatingState(String activatingState) {
        this.activatingState = activatingState;
    }

    public String getActiveState() {
        return activeState;
    }

    @Inject
    public void setActiveState(String activeState) {
        this.activeState = activeState;
    }

    public Set<String> getCancelStates() {
        return cancelStates;
    }

    @Inject
    public void setCancelStates(Set<String> cancelStates) {
        this.cancelStates = cancelStates;
    }

    public Set<String> getActiveStates() {
        return activeStates;
    }

    @Inject
    public void setActiveStates(Set<String> activeStates) {
        this.activeStates = activeStates;
    }

    public Set<String> getInactiveStates() {
        return inactiveStates;
    }

    @Inject
    public void setInactiveStates(Set<String> inactiveStates) {
        this.inactiveStates = inactiveStates;
    }

    public Set<String> getActivatingStates() {
        return activatingStates;
    }

    @Inject
    public void setActivatingStates(Set<String> activatingStates) {
        this.activatingStates = activatingStates;
    }

    public void setName(String name) {
        this.name = name;
    }

}