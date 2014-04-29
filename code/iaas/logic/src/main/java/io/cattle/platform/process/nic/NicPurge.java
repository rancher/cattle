package io.cattle.platform.process.nic;

import static io.cattle.platform.core.model.tables.NicTable.*;
import io.cattle.platform.core.dao.GenericMapDao;
import io.cattle.platform.core.model.IpAddress;
import io.cattle.platform.core.model.IpAddressNicMap;
import io.cattle.platform.core.model.Network;
import io.cattle.platform.core.model.Nic;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.process.base.AbstractDefaultProcessHandler;
import io.cattle.platform.resource.pool.ResourcePoolManager;
import io.cattle.platform.resource.pool.util.ResourcePoolConstants;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class NicPurge extends AbstractDefaultProcessHandler {

    GenericMapDao mapDao;
    ResourcePoolManager poolManager;

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Nic nic = (Nic)state.getResource();
        Network network = loadResource(Network.class, nic.getNetworkId());

        for ( IpAddressNicMap map : mapDao.findToRemove(IpAddressNicMap.class, Nic.class, nic.getId())) {
            IpAddress ipAddress = getObjectManager().loadResource(IpAddress.class, map.getIpAddressId());

            deactivateThenRemove(ipAddress, state.getData());
            deactivateThenRemove(map, state.getData());
        }

        if ( network != null ) {
            poolManager.releaseResource(network, ResourcePoolConstants.MAC, nic);
        }

        return new HandlerResult(NIC.MAC_ADDRESS, new Object[] { null }).withShouldContinue(true);
    }

    public GenericMapDao getMapDao() {
        return mapDao;
    }

    @Inject
    public void setMapDao(GenericMapDao mapDao) {
        this.mapDao = mapDao;
    }

    public ResourcePoolManager getPoolManager() {
        return poolManager;
    }

    @Inject
    public void setPoolManager(ResourcePoolManager poolManager) {
        this.poolManager = poolManager;
    }

}
