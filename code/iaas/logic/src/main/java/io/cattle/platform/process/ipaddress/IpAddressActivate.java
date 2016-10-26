package io.cattle.platform.process.ipaddress;

import static io.cattle.platform.core.model.tables.IpAddressTable.*;

import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.IpAddressConstants;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.IpAddress;
import io.cattle.platform.core.model.Network;
import io.cattle.platform.core.model.Nic;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.network.NetworkService;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.process.base.AbstractDefaultProcessHandler;
import io.cattle.platform.resource.pool.PooledResourceOptions;
import io.cattle.platform.resource.pool.ResourcePoolManager;
import io.cattle.platform.util.exception.ExecutionException;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;

@Named
public class IpAddressActivate extends AbstractDefaultProcessHandler {

    ResourcePoolManager poolManager;
    @Inject
    NetworkService networkService;

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        IpAddress ipAddress = (IpAddress) state.getResource();
        Network network = objectManager.loadResource(Network.class, ipAddress.getNetworkId());
        if (!networkService.shouldAssignIpAddress(network)) {
            return null;
        }

        String ip = allocateIp(ipAddress, network);

        return new HandlerResult(
                IP_ADDRESS.ADDRESS, ip,
                IP_ADDRESS.NAME, StringUtils.isBlank(ipAddress.getName()) ? ip : ipAddress.getName());
    }

    protected String allocateIp(IpAddress ipAddress, Network network) {
        Instance instance = getInstanceForPrimaryIp(ipAddress);
        String ip = null;
        String requestedIp = null;
        if (instance != null) {
            String allocatedIpAddress = DataAccessor
                    .fieldString(instance, InstanceConstants.FIELD_ALLOCATED_IP_ADDRESS);
            if (allocatedIpAddress != null) {
                ip = allocatedIpAddress;
            }
            requestedIp = DataAccessor.fieldString(instance, InstanceConstants.FIELD_REQUESTED_IP_ADDRESS);
        }

        if (ip == null) {
            ip = networkService.assignIpAddress(network, ipAddress, requestedIp);
            if (ip == null) {
                objectProcessManager.scheduleStandardProcess(StandardProcess.DEACTIVATE, ipAddress, null);
                throw new ExecutionException("IP allocation error", "Failed to allocate IP from subnet", ipAddress);
            }
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
