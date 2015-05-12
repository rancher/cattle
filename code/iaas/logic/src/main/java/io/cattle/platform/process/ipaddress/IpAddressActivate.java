package io.cattle.platform.process.ipaddress;

import static io.cattle.platform.core.model.tables.IpAddressTable.*;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.IpAddressConstants;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.IpAddress;
import io.cattle.platform.core.model.Nic;
import io.cattle.platform.core.model.Subnet;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.process.base.AbstractDefaultProcessHandler;
import io.cattle.platform.resource.pool.PooledResource;
import io.cattle.platform.resource.pool.PooledResourceOptions;
import io.cattle.platform.resource.pool.ResourcePoolManager;
import io.cattle.platform.util.exception.ExecutionException;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;

@Named
public class IpAddressActivate extends AbstractDefaultProcessHandler {

    ResourcePoolManager poolManager;

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        IpAddress ipAddress = (IpAddress) state.getResource();
        Subnet subnet = getObjectManager().loadResource(Subnet.class, ipAddress.getSubnetId());

        if (subnet == null) {
            return null;
        }

        if (!CommonStatesConstants.ACTIVE.equals(subnet.getState())) {
            getObjectProcessManager().scheduleStandardProcess(StandardProcess.DEACTIVATE, ipAddress, null);
            throw new ExecutionException("IP allocation error", "Subnet not active", ipAddress);
        }


        PooledResourceOptions options = getPoolOptions(ipAddress);
        PooledResource resource = poolManager.allocateOneResource(subnet, ipAddress, options);

        if (resource == null) {
            getObjectProcessManager().scheduleStandardProcess(StandardProcess.DEACTIVATE, ipAddress, null);
            throw new ExecutionException("IP allocation error", "Failed to allocate IP from subnet", ipAddress);
        }

        Long networkId = ipAddress.getNetworkId();

        if (networkId == null && subnet != null) {
            networkId = subnet.getNetworkId();
        }

        return new HandlerResult(IP_ADDRESS.ADDRESS, resource.getName(), IP_ADDRESS.NAME, StringUtils.isBlank(ipAddress.getName()) ? resource.getName()
                : ipAddress.getName(), IP_ADDRESS.NETWORK_ID, networkId);
    }

    protected PooledResourceOptions getPoolOptions(IpAddress ipAddress) {
        PooledResourceOptions options = new PooledResourceOptions();

        if (IpAddressConstants.ROLE_PRIMARY.equals(ipAddress.getRole())) {
            for (Nic nic : getObjectManager().mappedChildren(ipAddress, Nic.class)) {
                if (nic.getDeviceNumber() != null && nic.getDeviceNumber() == 0) {
                    Instance instance = getObjectManager().loadResource(Instance.class, nic.getInstanceId());
                    if (instance == null) {
                        continue;
                    }

                    String ip = DataAccessor.fieldString(instance, InstanceConstants.FIELD_REQUESTED_IP_ADDRESS);
                    if (ip != null) {
                        options.setRequestedItem(ip);
                        break;
                    }
                }
            }
        }

        return options;
    }

    public ResourcePoolManager getPoolManager() {
        return poolManager;
    }

    @Inject
    public void setPoolManager(ResourcePoolManager poolManager) {
        this.poolManager = poolManager;
    }

}
