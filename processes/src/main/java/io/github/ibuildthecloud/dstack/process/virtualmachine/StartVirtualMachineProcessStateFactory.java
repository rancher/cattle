package io.github.ibuildthecloud.dstack.process.virtualmachine;

import javax.inject.Inject;

import io.github.ibuildthecloud.dstack.engine.process.LaunchConfiguration;
import io.github.ibuildthecloud.dstack.engine.process.ProcessState;
import io.github.ibuildthecloud.dstack.engine.process.ProcessStateFactory;
import io.github.ibuildthecloud.dstack.object.ObjectManager;

public class StartVirtualMachineProcessStateFactory implements ProcessStateFactory {

    ObjectManager objectManager;

    @Override
    public String getName() {
        return "START_VIRTUAL_MACHINE";
    }

    @Override
    public ProcessState constructProcessState(LaunchConfiguration config) {
        return new StartVirtualMachineProcessState(config, objectManager);
    }

    public ObjectManager getObjectManager() {
        return objectManager;
    }

    @Inject
    public void setObjectManager(ObjectManager objectManager) {
        this.objectManager = objectManager;
    }

}
