package io.cattle.platform.process.network;

import io.cattle.platform.core.constants.NetworkConstants;
import io.cattle.platform.core.model.Network;
import io.cattle.platform.core.model.Subnet;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.process.base.AbstractDefaultProcessHandler;
import io.cattle.platform.resource.pool.PooledResourceOptions;
import io.cattle.platform.resource.pool.ResourcePoolManager;
import io.cattle.platform.resource.pool.util.ResourcePoolConstants;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class NetworkRemove extends AbstractDefaultProcessHandler {

    @Inject
    ResourcePoolManager resourcePoolManager;

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Network network = (Network)state.getResource();
        for (Subnet subnet : objectManager.children(network, Subnet.class)) {
            deactivateThenRemove(subnet, null);
        }

        resourcePoolManager.releaseResource(ResourcePoolManager.GLOBAL, network, new PooledResourceOptions().withQualifier(ResourcePoolConstants.MAC_PREFIX));
        return new HandlerResult(NetworkConstants.FIELD_MAC_PREFIX, new Object[] { null }).withShouldContinue(true);
    }

}