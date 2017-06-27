package io.cattle.platform.process.instance;

import io.cattle.platform.agent.AgentLocator;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.serialization.ObjectSerializerFactory;
import io.cattle.platform.process.common.handler.AgentBasedProcessHandler;

public class InstanceRemove extends AgentBasedProcessHandler {

    public InstanceRemove(AgentLocator agentLocator, ObjectSerializerFactory factory, ObjectManager objectManager, ObjectProcessManager processManager) {
        super(agentLocator, factory, objectManager, processManager);
        commandName = "compute.instance.remove";
        dataTypeClass = Instance.class;
        shortCircuitIfAgentRemoved = true;
    }

}
