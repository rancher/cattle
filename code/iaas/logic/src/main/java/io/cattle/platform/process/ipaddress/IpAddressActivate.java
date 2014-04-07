package io.cattle.platform.process.ipaddress;

import static io.cattle.platform.core.model.tables.IpAddressTable.*;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.model.IpAddress;
import io.cattle.platform.core.model.Subnet;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.process.base.AbstractDefaultProcessHandler;
import io.cattle.platform.resource.pool.PooledResource;
import io.cattle.platform.resource.pool.ResourcePoolManager;
import io.cattle.platform.util.exception.ExecutionException;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class IpAddressActivate extends AbstractDefaultProcessHandler {

    ResourcePoolManager poolManager;

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        IpAddress ipAddress = (IpAddress)state.getResource();
        Subnet subnet = getObjectManager().loadResource(Subnet.class, ipAddress.getSubnetId());

        if ( subnet == null ) {
            return null;
        }

        if ( ! CommonStatesConstants.ACTIVE.equals(subnet.getState()) ) {
            getObjectProcessManager().scheduleStandardProcess(StandardProcess.DEACTIVATE, ipAddress, null);
            throw new ExecutionException("IP allocation error", "Subnet not active", ipAddress);
        }

        PooledResource resource = poolManager.allocateResource(subnet, ipAddress);

        if ( resource == null ) {
            getObjectProcessManager().scheduleStandardProcess(StandardProcess.DEACTIVATE, ipAddress, null);
            throw new ExecutionException("IP allocation error", "Failed to allocate IP from subnet", ipAddress);
        }

        Long networkId = ipAddress.getNetworkId();

        if ( networkId == null && subnet != null ) {
            networkId = subnet.getNetworkId();
        }

        return new HandlerResult(IP_ADDRESS.ADDRESS, resource.getName(),
                IP_ADDRESS.NETWORK_ID, networkId);
    }

    public ResourcePoolManager getPoolManager() {
        return poolManager;
    }

    @Inject
    public void setPoolManager(ResourcePoolManager poolManager) {
        this.poolManager = poolManager;
    }

}
