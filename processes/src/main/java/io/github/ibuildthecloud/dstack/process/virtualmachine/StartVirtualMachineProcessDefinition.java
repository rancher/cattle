package io.github.ibuildthecloud.dstack.process.virtualmachine;

import io.github.ibuildthecloud.dstack.engine.process.AbstractProcessDefinition;
import io.github.ibuildthecloud.dstack.engine.process.ProcessDefinition;

public class StartVirtualMachineProcessDefinition extends AbstractProcessDefinition implements ProcessDefinition {

    @Override
    public String getName() {
        return "START_VIRTUAL_MACHINE";
    }

}
