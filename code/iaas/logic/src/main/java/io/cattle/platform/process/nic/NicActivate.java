package io.cattle.platform.process.nic;

import static io.cattle.platform.core.model.tables.IpAddressTable.*;
import static io.cattle.platform.core.model.tables.NicTable.*;
import io.cattle.platform.core.constants.IpAddressConstants;
import io.cattle.platform.core.dao.GenericMapDao;
import io.cattle.platform.core.dao.IpAddressDao;
import io.cattle.platform.core.dao.VnetDao;
import io.cattle.platform.core.model.IpAddress;
import io.cattle.platform.core.model.IpAddressNicMap;
import io.cattle.platform.core.model.Network;
import io.cattle.platform.core.model.Nic;
import io.cattle.platform.core.model.Subnet;
import io.cattle.platform.core.model.SubnetVnetMap;
import io.cattle.platform.core.model.Vnet;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.process.base.AbstractDefaultProcessHandler;
import io.cattle.platform.resource.pool.PooledResource;
import io.cattle.platform.resource.pool.PooledResourceOptions;
import io.cattle.platform.resource.pool.ResourcePoolManager;
import io.cattle.platform.resource.pool.util.ResourcePoolConstants;
import io.cattle.platform.util.exception.ExecutionException;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class NicActivate extends AbstractDefaultProcessHandler {

    GenericMapDao mapDao;
    VnetDao vnetDao;
    IpAddressDao ipAddressDao;
    ResourcePoolManager poolManager;

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Nic nic = (Nic) state.getResource();
        Network network = getObjectManager().loadResource(Network.class, nic.getNetworkId());
        Subnet subnet = getObjectManager().loadResource(Subnet.class, nic.getSubnetId());

        if (network == null) {
            return null;
        }

        activate(network, state.getData());

        Vnet vnet = getVnet(nic, subnet);
        if (vnet != null) {
            activate(vnet, state.getData());
        }

        IpAddress ipAddress = getIpAddress(nic, subnet);

        if (ipAddress != null) {
            activate(ipAddress, state.getData());
        }

        String mac = assignMacAddress(network, nic);

        return new HandlerResult(NIC.VNET_ID, nic.getVnetId(), NIC.MAC_ADDRESS, mac);
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

    protected IpAddress getIpAddress(Nic nic, Subnet subnet) {
        IpAddress ipAddress = ipAddressDao.getPrimaryIpAddress(nic);

        if (ipAddress == null && nic.getSubnetId() != null) {
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

    protected Vnet getVnet(Nic nic, Subnet subnet) {
        Vnet vnet = getObjectManager().loadResource(Vnet.class, nic.getVnetId());

        if (vnet != null) {
            return vnet;
        }

        vnet = lookupVnet(nic, subnet);

        if (vnet != null) {
            getObjectManager().setFields(nic, NIC.VNET_ID, vnet.getId());
        }

        return vnet;
    }

    protected Vnet lookupVnet(Nic nic, Subnet subnet) {
        if (subnet == null) {
            return null;
        }

        List<? extends SubnetVnetMap> vnets = mapDao.findNonRemoved(SubnetVnetMap.class, Subnet.class, subnet.getId());

        if (vnets.size() == 0) {
            return null;
        }

        if (vnets.size() == 1) {
            return getObjectManager().loadResource(Vnet.class, vnets.get(0).getVnetId());
        }

        return vnetDao.findVnetFromHosts(nic.getInstanceId(), subnet.getId());
    }

    public GenericMapDao getMapDao() {
        return mapDao;
    }

    @Inject
    public void setMapDao(GenericMapDao mapDao) {
        this.mapDao = mapDao;
    }

    public VnetDao getVnetDao() {
        return vnetDao;
    }

    @Inject
    public void setVnetDao(VnetDao vnetDao) {
        this.vnetDao = vnetDao;
    }

    public IpAddressDao getIpAddressDao() {
        return ipAddressDao;
    }

    @Inject
    public void setIpAddressDao(IpAddressDao ipAddressDao) {
        this.ipAddressDao = ipAddressDao;
    }

    public ResourcePoolManager getPoolManager() {
        return poolManager;
    }

    @Inject
    public void setPoolManager(ResourcePoolManager poolManager) {
        this.poolManager = poolManager;
    }

}
