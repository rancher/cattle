package io.cattle.platform.agent.instance.link.process;

import io.cattle.platform.agent.instance.service.AgentInstanceManager;
import io.cattle.platform.agent.instance.service.NetworkServiceInfo;
import io.cattle.platform.core.constants.InstanceLinkConstants;
import io.cattle.platform.core.constants.NetworkServiceConstants;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.InstanceLink;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.process.common.handler.AbstractObjectProcessHandler;
import io.cattle.platform.resource.pool.PooledResourceOptions;
import io.cattle.platform.resource.pool.ResourcePoolManager;
import io.cattle.platform.resource.pool.util.ResourcePoolConstants;

import java.util.ArrayList;

import javax.inject.Inject;

public class AgentInstanceLinkPurge extends AbstractObjectProcessHandler {

    AgentInstanceManager agentInstanceManager;
    ResourcePoolManager resourcePoolManager;

    @Override
    public String[] getProcessNames() {
        return new String[] { "instancelink.purge" };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        InstanceLink link = (InstanceLink)state.getResource();
        Instance instance = loadResource(Instance.class, link.getInstanceId());

        if ( instance == null ) {
            return null;
        }

        NetworkServiceInfo info = agentInstanceManager.getNetworkService(instance, NetworkServiceConstants.KIND_LINK, false);

        if ( info == null ) {
            return null;
        }

        resourcePoolManager.releaseResource(info.getNetworkService(), link,
                new PooledResourceOptions().withQualifier(ResourcePoolConstants.LINK_PORT));

        return new HandlerResult(InstanceLinkConstants.FIELD_PORTS, new ArrayList<Object>()).withShouldContinue(true);
    }

    public AgentInstanceManager getAgentInstanceManager() {
        return agentInstanceManager;
    }

    @Inject
    public void setAgentInstanceManager(AgentInstanceManager agentInstanceManager) {
        this.agentInstanceManager = agentInstanceManager;
    }

    public ResourcePoolManager getResourcePoolManager() {
        return resourcePoolManager;
    }

    @Inject
    public void setResourcePoolManager(ResourcePoolManager resourcePoolManager) {
        this.resourcePoolManager = resourcePoolManager;
    }

}
