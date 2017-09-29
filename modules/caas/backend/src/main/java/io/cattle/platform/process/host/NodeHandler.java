package io.cattle.platform.process.host;

import io.cattle.platform.agent.AgentLocator;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.util.SystemLabels;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.metadata.MetadataManager;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.serialization.ObjectSerializer;
import io.cattle.platform.process.common.handler.AgentBasedProcessHandler;

import java.util.List;

public class NodeHandler extends AgentBasedProcessHandler {
    MetadataManager metadataManager;

    public NodeHandler(AgentLocator agentLocator, ObjectSerializer serializer, ObjectManager objectManager, ObjectProcessManager processManager,
            MetadataManager metadataManager) {
        super(agentLocator, serializer, objectManager, processManager, null);
        this.metadataManager = metadataManager;
        shortCircuitIfAgentRemoved = true;
    }

    @Override
    protected Object getAgentResource(ProcessState state, ProcessInstance process, Object dataResource) {
        Host host = (Host)state.getResource();
        List<Long> agentIds = metadataManager.getAgentProvider(SystemLabels.LABEL_K8S_AGENT, host.getClusterId());
        if (agentIds.isEmpty()) {
            return null;
        }
        return agentIds.get(0);
    }
}
