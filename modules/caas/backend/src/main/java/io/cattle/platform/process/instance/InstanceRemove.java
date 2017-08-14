package io.cattle.platform.process.instance;

import io.cattle.platform.agent.AgentLocator;
import io.cattle.platform.metadata.MetadataManager;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.serialization.ObjectSerializer;

public class InstanceRemove extends DeploymentSyncRequestHandler {

    public InstanceRemove(AgentLocator agentLocator, ObjectSerializer serializer, ObjectManager objectManager, ObjectProcessManager processManager, DeploymentSyncFactory syncFactory, MetadataManager metadataManager) {
        super(agentLocator, serializer, objectManager, processManager, syncFactory, metadataManager);
        shortCircuitIfAgentRemoved = true;
        this.commandName = "compute.instance.remove";
        this.externalAlways = true;
    }

}
