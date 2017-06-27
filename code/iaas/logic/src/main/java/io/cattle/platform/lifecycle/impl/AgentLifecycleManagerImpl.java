package io.cattle.platform.lifecycle.impl;

import static io.cattle.platform.object.util.DataAccessor.*;

import io.cattle.platform.agent.instance.factory.AgentInstanceFactory;
import io.cattle.platform.core.constants.AgentConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.util.SystemLabels;
import io.cattle.platform.lifecycle.AgentLifecycleManager;
import io.cattle.platform.object.util.DataAccessor;

import java.util.List;

public class AgentLifecycleManagerImpl implements AgentLifecycleManager {

    AgentInstanceFactory agentInstanceFactory;

    public AgentLifecycleManagerImpl(AgentInstanceFactory agentInstanceFactory) {
        this.agentInstanceFactory = agentInstanceFactory;
    }

    @Override
    public void create(Instance instance) {
        Agent agent = agentInstanceFactory.createAgent(instance);
        if (agent != null) {
            instance.setAgentId(agent.getId());
        }
        setAgentVolumes(instance);
    }

    protected void setAgentVolumes(Instance instance) {
        if (!"true".equalsIgnoreCase(getLabel(instance, SystemLabels.LABEL_AGENT_CREATE))) {
            return;
        }

        List<String> dataVolumes = DataAccessor.appendToFieldStringList(instance,
                InstanceConstants.FIELD_DATA_VOLUMES,
                AgentConstants.AGENT_INSTANCE_BIND_MOUNT);

        setField(instance, InstanceConstants.FIELD_DATA_VOLUME_MOUNTS, dataVolumes);
    }

    @Override
    public void preRemove(Instance instance) {
        agentInstanceFactory.deleteAgent(instance);
    }

}
