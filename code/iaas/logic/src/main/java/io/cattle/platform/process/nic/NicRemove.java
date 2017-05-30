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
import io.cattle.platform.resource.pool.PooledResourceOptions;
import io.cattle.platform.resource.pool.ResourcePoolManager;
import io.cattle.platform.resource.pool.util.ResourcePoolConstants;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class NicRemove extends AbstractDefaultProcessHandler {

    @Inject
    GenericMapDao mapDao;
    @Inject
    ResourcePoolManager poolManager;

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        /* Release requested IPs on delete, not on purge */
        Nic nic = (Nic) state.getResource();

        for (IpAddressNicMap map : mapDao.findToRemove(IpAddressNicMap.class, Nic.class, nic.getId())) {
            IpAddress ipAddress = getObjectManager().loadResource(IpAddress.class, map.getIpAddressId());

            /* Deactivate to release the IP address */
            deactivateThenRemove(ipAddress, state.getData());
        }

        Network network = loadResource(Network.class, nic.getNetworkId());

        if (network != null) {
            poolManager.releaseResource(network, nic, new PooledResourceOptions().withQualifier(ResourcePoolConstants.MAC));
        }

        return new HandlerResult(NIC.MAC_ADDRESS, new Object[] { null }).withShouldContinue(true);
    }

}
