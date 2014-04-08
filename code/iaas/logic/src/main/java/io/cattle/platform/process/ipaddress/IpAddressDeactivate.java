package io.cattle.platform.process.ipaddress;

import io.cattle.platform.core.model.IpAddress;
import io.cattle.platform.core.model.Subnet;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.process.base.AbstractDefaultProcessHandler;
import io.cattle.platform.resource.pool.ResourcePoolManager;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class IpAddressDeactivate extends AbstractDefaultProcessHandler {

    ResourcePoolManager poolManager;

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        IpAddress ipAddress = (IpAddress)state.getResource();
        Subnet subnet = getObjectManager().loadResource(Subnet.class, ipAddress.getSubnetId());

        if ( subnet == null ) {
            return null;
        }

        poolManager.releaseResource(subnet, ipAddress);

        return null;
    }

    public ResourcePoolManager getPoolManager() {
        return poolManager;
    }

    @Inject
    public void setPoolManager(ResourcePoolManager poolManager) {
        this.poolManager = poolManager;
    }

}
