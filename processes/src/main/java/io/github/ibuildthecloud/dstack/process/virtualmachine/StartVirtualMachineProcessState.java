package io.github.ibuildthecloud.dstack.process.virtualmachine;

import io.github.ibuildthecloud.dstack.db.jooq.generated.tables.records.InstanceRecord;
import io.github.ibuildthecloud.dstack.engine.process.LaunchConfiguration;
import io.github.ibuildthecloud.dstack.engine.process.ProcessState;
import io.github.ibuildthecloud.dstack.lock.definition.LockDefinition;
import io.github.ibuildthecloud.dstack.object.ObjectManager;
import io.github.ibuildthecloud.dstack.process.lock.ResourceChangeLock;
import io.github.ibuildthecloud.dstack.process.lock.UniqueStateChangeLock;

public class StartVirtualMachineProcessState implements ProcessState {

    InstanceRecord instance;
    ObjectManager objectManager;
    LockDefinition stateChangeLock = new UniqueStateChangeLock();

    public StartVirtualMachineProcessState(LaunchConfiguration config, ObjectManager objectManager) {
        super();
        this.objectManager = objectManager;
        this.instance = objectManager.loadResource(config.getResourceType(), config.getResourceId());
    }

    @Override
    public void setActivating() {
        instance.setState("starting");
        instance = objectManager.persist(instance);
    }

    @Override
    public void setActive() {
        instance.setState("running");
        instance = objectManager.persist(instance);
    }

    @Override
    public LockDefinition getProcessLock() {
        return new ResourceChangeLock("instance", instance.getId());
    }

    @Override
    public LockDefinition getStateChangeLock() {
        return stateChangeLock;
    }

    @Override
    public boolean shouldCancel() {
        return false;
    }

    @Override
    public boolean isDone() {
        return instance.getState().equals("running");
    }

    @Override
    public boolean isStart() {
        return instance.getState().equals("requested");
    }

    @Override
    public boolean isTransitioning() {
        return instance.getState().equals("starting");
    }

    @Override
    public void reload() {
        instance = objectManager.reload(instance);
    }

    @Override
    public Object getResource() {
        return instance;
    }

}
