package io.cattle.platform.process.network;

import io.cattle.platform.core.constants.NetworkConstants;
import io.cattle.platform.core.model.Network;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPostListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;
import io.cattle.platform.resource.pool.ResourcePoolManager;
import io.cattle.platform.resource.pool.util.ResourcePoolConstants;
import io.cattle.platform.util.type.Priority;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class MacAddressNetworkPurge extends AbstractObjectProcessLogic implements ProcessPostListener, Priority {

    ResourcePoolManager resourcePoolManager;

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Network network = (Network)state.getResource();

        resourcePoolManager.releaseResource(ResourcePoolManager.GLOBAL, ResourcePoolConstants.MAC_PREFIX, network);
        return new HandlerResult(NetworkConstants.FIELD_MAC_PREFIX, new Object[] { null }).withShouldContinue(true);
    }

    @Override
    public String[] getProcessNames() {
        return new String[] { "network.purge" };
    }

    @Override
    public int getPriority() {
        return Priority.DEFAULT;
    }

    public ResourcePoolManager getResourcePoolManager() {
        return resourcePoolManager;
    }

    @Inject
    public void setResourcePoolManager(ResourcePoolManager resourcePoolManager) {
        this.resourcePoolManager = resourcePoolManager;
    }

}