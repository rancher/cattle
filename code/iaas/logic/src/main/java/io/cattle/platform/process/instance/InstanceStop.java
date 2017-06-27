package io.cattle.platform.process.instance;

import io.cattle.platform.agent.AgentLocator;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.serialization.ObjectSerializerFactory;
import io.cattle.platform.process.common.handler.AgentBasedProcessHandler;

import java.util.Arrays;

public class InstanceStop extends AgentBasedProcessHandler {

    public InstanceStop(AgentLocator agentLocator, ObjectSerializerFactory factory, ObjectManager objectManager, ObjectProcessManager processManager) {
        super(agentLocator, factory, objectManager, processManager);
        commandName = "compute.instance.deactivate";
        dataTypeClass = Instance.class;
        shortCircuitIfAgentRemoved = true;
        processDataKeys = Arrays.asList("timeout", "containerNoOpEvent");
    }

}
