package io.cattle.platform.process.instance;

import io.cattle.platform.agent.AgentLocator;
import io.cattle.platform.environment.EnvironmentResourceManager;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.serialization.ObjectSerializer;

import java.util.Arrays;

public class InstanceStop extends DeploymentSyncRequestHandler {

    public InstanceStop(AgentLocator agentLocator, ObjectSerializer serializer, ObjectManager objectManager, ObjectProcessManager processManager, DeploymentSyncFactory syncFactory, EnvironmentResourceManager envResourceManager) {
        super(agentLocator, serializer, objectManager, processManager, syncFactory, envResourceManager);
        shortCircuitIfAgentRemoved = true;
        commandName = "compute.instance.deactivate";
        processDataKeys = Arrays.asList("timeout", "containerNoOpEvent");
    }

}
