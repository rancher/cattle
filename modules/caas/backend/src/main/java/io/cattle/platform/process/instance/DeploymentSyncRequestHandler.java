package io.cattle.platform.process.instance;

import io.cattle.platform.agent.AgentLocator;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.serialization.ObjectSerializer;
import io.cattle.platform.process.common.handler.AgentBasedProcessHandler;

public class DeploymentSyncRequestHandler extends AgentBasedProcessHandler {

    DeploymentSyncFactory syncFactory;

    public DeploymentSyncRequestHandler(AgentLocator agentLocator, ObjectSerializer serializer, ObjectManager objectManager, ObjectProcessManager processManager, DeploymentSyncFactory syncFactory) {
        super(agentLocator, serializer, objectManager, processManager);
        this.syncFactory = syncFactory;
        this.commandName = "compute.sync";
    }

    @Override
    protected Object getDataResource(ProcessState state, ProcessInstance process) {
        return syncFactory.construct((Instance) state.getResource());
    }

}
