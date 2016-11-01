package io.cattle.platform.process.network;

import io.cattle.platform.core.constants.NetworkConstants;
import io.cattle.platform.core.model.Network;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPreListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;
import io.cattle.platform.resource.pool.PooledResource;
import io.cattle.platform.resource.pool.PooledResourceOptions;
import io.cattle.platform.resource.pool.ResourcePoolManager;
import io.cattle.platform.resource.pool.util.ResourcePoolConstants;
import io.cattle.platform.util.exception.ExecutionException;
import io.cattle.platform.util.type.Priority;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;

@Named
public class MacAddressNetworkActivate extends AbstractObjectProcessLogic implements ProcessPreListener, Priority {

    ResourcePoolManager resourcePoolManager;

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Network network = (Network) state.getResource();

        String field = DataAccessor.field(network, NetworkConstants.FIELD_MAC_PREFIX, String.class);

        if (StringUtils.isBlank(field)) {
            PooledResource mac = resourcePoolManager.allocateOneResource(ResourcePoolManager.GLOBAL, network, new PooledResourceOptions()
                    .withQualifier(ResourcePoolConstants.MAC_PREFIX));
            if (mac == null) {
                throw new ExecutionException("Mac prefix allocation error", "Failed to get mac prefix", network);
            }
            field = mac.getName();
        }

        return new HandlerResult(NetworkConstants.FIELD_MAC_PREFIX, field).withShouldContinue(true);
    }

    @Override
    public String[] getProcessNames() {
        return new String[] { "network.activate" };
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