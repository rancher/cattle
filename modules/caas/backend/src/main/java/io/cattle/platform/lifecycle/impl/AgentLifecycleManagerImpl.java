package io.cattle.platform.lifecycle.impl;

import com.google.common.util.concurrent.ListenableFuture;
import io.cattle.platform.agent.instance.factory.AgentInstanceFactory;
import io.cattle.platform.async.utils.AsyncUtils;
import io.cattle.platform.core.constants.AgentConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.util.SystemLabels;
import io.cattle.platform.lifecycle.AgentLifecycleManager;
import io.cattle.platform.object.resource.ResourceMonitor;
import io.cattle.platform.object.util.DataAccessor;

import java.util.List;

import static io.cattle.platform.object.util.DataAccessor.*;

public class AgentLifecycleManagerImpl implements AgentLifecycleManager {

    AgentInstanceFactory agentInstanceFactory;
    ResourceMonitor resourceMonitor;

    public AgentLifecycleManagerImpl(AgentInstanceFactory agentInstanceFactory, ResourceMonitor resourceMonitor) {
        this.agentInstanceFactory = agentInstanceFactory;
        this.resourceMonitor = resourceMonitor;
    }

    @Override
    public ListenableFuture<Agent> create(Instance instance) {
        if (instance.getNativeContainer()) {
            return AsyncUtils.done();
        }

        Agent agent = agentInstanceFactory.createAgent(instance);
        if (agent != null) {
            instance.setAgentId(agent.getId());
        }
        setAgentVolumes(instance);

        return resourceMonitor.waitFor(agent, "agent credentials active", agentInstanceFactory::areAllCredentialsActive);
    }

    protected void setAgentVolumes(Instance instance) {
        if (!"true".equalsIgnoreCase(getLabel(instance, SystemLabels.LABEL_AGENT_CREATE))) {
            return;
        }

        List<String> dataVolumes = DataAccessor.appendToFieldStringList(instance,
                InstanceConstants.FIELD_DATA_VOLUMES,
                AgentConstants.AGENT_INSTANCE_BIND_MOUNT);

        setField(instance, InstanceConstants.FIELD_DATA_VOLUMES, dataVolumes);
    }

    @Override
    public void preRemove(Instance instance) {
        agentInstanceFactory.deleteAgent(instance);
    }

}
