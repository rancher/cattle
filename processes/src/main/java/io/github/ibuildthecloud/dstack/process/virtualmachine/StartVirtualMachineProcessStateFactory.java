package io.github.ibuildthecloud.dstack.process.virtualmachine;

import io.github.ibuildthecloud.dstack.engine.process.LaunchConfiguration;
import io.github.ibuildthecloud.dstack.engine.process.ProcessState;
import io.github.ibuildthecloud.dstack.engine.process.ProcessStateFactory;

public class StartVirtualMachineProcessStateFactory implements ProcessStateFactory {

    @Override
    public String getName() {
        return "START_VIRTUAL_MACHINE";
    }

    @Override
    public ProcessState constructProcessState(LaunchConfiguration config) {
        return new StartVirtualMachineProcessState();
    }

}
