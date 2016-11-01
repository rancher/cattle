package io.cattle.platform.process.nic;

import static io.cattle.platform.core.model.tables.IpAddressTable.*;
import static io.cattle.platform.core.model.tables.NicTable.*;

import io.cattle.platform.core.constants.IpAddressConstants;
import io.cattle.platform.core.dao.GenericMapDao;
import io.cattle.platform.core.dao.IpAddressDao;
import io.cattle.platform.core.model.IpAddress;
import io.cattle.platform.core.model.IpAddressNicMap;
import io.cattle.platform.core.model.Network;
import io.cattle.platform.core.model.Nic;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.network.NetworkService;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.process.base.AbstractDefaultProcessHandler;
import io.cattle.platform.resource.pool.PooledResource;
import io.cattle.platform.resource.pool.PooledResourceOptions;
import io.cattle.platform.resource.pool.ResourcePoolManager;
import io.cattle.platform.resource.pool.util.ResourcePoolConstants;
import io.cattle.platform.util.exception.ExecutionException;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class NicActivate extends AbstractDefaultProcessHandler {

    @Inject
    GenericMapDao mapDao;
    @Inject
    IpAddressDao ipAddressDao;
    @Inject
    ResourcePoolManager poolManager;
    @Inject
    NetworkService networkService;

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Nic nic = (Nic) state.getResource();
        Network network = getObjectManager().loadResource(Network.class, nic.getNetworkId());

        if (network == null) {
            return null;
        }

        IpAddress ipAddress = getIpAddress(nic, network);

        if (ipAddress != null) {
            activate(ipAddress, state.getData());
        }

        String mac = assignMacAddress(network, nic);

        return new HandlerResult(NIC.MAC_ADDRESS, mac);
    }

    protected String assignMacAddress(Network network, Nic nic) {
        String mac = nic.getMacAddress();

        if (mac != null) {
            return mac;
        }

        PooledResource resource = poolManager.allocateOneResource(network, nic, new PooledResourceOptions().withQualifier(ResourcePoolConstants.MAC));
        if (resource == null) {
            throw new ExecutionException("MAC allocation error", "Failed to allocate MAC from network", nic);
        }

        return resource.getName();
    }

    protected IpAddress getIpAddress(Nic nic, Network network) {
        IpAddress ipAddress = ipAddressDao.getPrimaryIpAddress(nic);

        if (ipAddress == null && networkService.shouldAssignIpAddress(network)) {
            ipAddress = ipAddressDao.mapNewIpAddress(nic, IP_ADDRESS.ROLE, IpAddressConstants.ROLE_PRIMARY);
        }

        for (IpAddressNicMap map : mapDao.findNonRemoved(IpAddressNicMap.class, Nic.class, nic.getId())) {
            getObjectProcessManager().executeStandardProcess(StandardProcess.CREATE, map, null);
        }

        if (ipAddress != null) {
            getObjectProcessManager().executeStandardProcess(StandardProcess.CREATE, ipAddress, null);
        }

        return ipAddress;
    }

}
