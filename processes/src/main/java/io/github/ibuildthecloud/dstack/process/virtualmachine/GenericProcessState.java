package io.github.ibuildthecloud.dstack.process.virtualmachine;

import io.github.ibuildthecloud.dstack.engine.process.LaunchConfiguration;
import io.github.ibuildthecloud.dstack.engine.process.ProcessState;
import io.github.ibuildthecloud.dstack.lock.definition.LockDefinition;

public class GenericProcessState<T extends GenericProcessStateFactory> implements ProcessState {

    String resourceType, resourceId;
    Object resource = null;
    T factory;
    LockDefinition processLock;
    LockDefinition stateChangeLock;

    public GenericProcessState(LaunchConfiguration config, T factory) {
        super();
        this.resourceType = config.getResourceType();
        this.resourceId = config.getResourceId();
        this.factory = factory;
        this.processLock = new GenericProcessLock(resourceType, resourceId);
        this.stateChangeLock = new GenericStateChangeLock(resourceType, resourceId);
    }

    @Override
    public void setActivating() {
        setState(factory.getActivatingState());
    }

    @Override
    public void setActive() {
        setState(factory.getActiveState());
    }
    
    protected void setState(String state) {
        factory.getDataAccess().setState(getResource(), factory.getStateField(), state);
    }
    
    protected String getState() {
        return factory.getDataAccess().getState(getResource(), factory.getStateField());
    }

    @Override
    public LockDefinition getProcessLock() {
        return processLock;
    }

    @Override
    public LockDefinition getStateChangeLock() {
        return stateChangeLock;
    }

    @Override
    public boolean shouldCancel() {
        return factory.getCancelStates().contains(getState());
    }

    @Override
    public boolean isDone() {
        return factory.getActiveStates().contains(getState());
    }

    @Override
    public boolean isStart() {
        return factory.getInactiveStates().contains(getState());
    }

    @Override
    public boolean isTransitioning() {
        return factory.getActivatingStates().contains(getState());
    }

    @Override
    public void reload() {
        resource = factory.getDataAccess().load(resourceType, resourceId);
    }

    @Override
    public Object getResource() {
        if ( resource == null ) {
            reload();
        }
        return resource;
    }

}
