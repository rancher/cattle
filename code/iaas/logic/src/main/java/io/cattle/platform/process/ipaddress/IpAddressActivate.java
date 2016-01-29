package io.cattle.platform.process.ipaddress;

import static io.cattle.platform.core.model.tables.IpAddressTable.IP_ADDRESS;
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

        String ip = allocateIp(ipAddress, subnet);

        Long networkId = ipAddress.getNetworkId();

        if (networkId == null && subnet != null) {
            networkId = subnet.getNetworkId();
        }

        return new HandlerResult(IP_ADDRESS.ADDRESS, ip, IP_ADDRESS.NAME, StringUtils.isBlank(ipAddress.getName()) ? ip
                : ipAddress.getName(), IP_ADDRESS.NETWORK_ID, networkId);
    }

    protected String allocateIp(IpAddress ipAddress, Subnet subnet) {
        Instance instance = getInstanceForPrimaryIp(ipAddress);
        String ip = null;
        if (instance != null) {
            String allocatedIpAddress = DataAccessor
                    .fieldString(instance, InstanceConstants.FIELD_ALLOCATED_IP_ADDRESS);
            if (allocatedIpAddress != null) {
                ip = allocatedIpAddress;
            }
        }

        if (ip == null) {
            PooledResourceOptions options = getPoolOptions(ipAddress, instance);
            PooledResource resource = poolManager.allocateOneResource(subnet, ipAddress, options);

            if (resource == null) {
                getObjectProcessManager().scheduleStandardProcess(StandardProcess.DEACTIVATE, ipAddress, null);
                throw new ExecutionException("IP allocation error", "Failed to allocate IP from subnet", ipAddress);
            }
            ip = resource.getName();
        }
        return ip;
    }

    protected PooledResourceOptions getPoolOptions(IpAddress ipAddress, Instance instance) {
        PooledResourceOptions options = new PooledResourceOptions();
        if (instance != null) {
            String ip = DataAccessor.fieldString(instance, InstanceConstants.FIELD_REQUESTED_IP_ADDRESS);
            if (ip != null) {
                options.setRequestedItem(ip);
            }
        }

        return options;
    }

    protected Instance getInstanceForPrimaryIp(IpAddress ipAddress) {
        if (IpAddressConstants.ROLE_PRIMARY.equals(ipAddress.getRole())) {
            for (Nic nic : getObjectManager().mappedChildren(ipAddress, Nic.class)) {
                if (nic.getDeviceNumber() != null && nic.getDeviceNumber() == 0) {
                    Instance instance = getObjectManager().loadResource(Instance.class, nic.getInstanceId());
                    if (instance != null) {
                        return instance;
                    }
                }
            }
        }
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
