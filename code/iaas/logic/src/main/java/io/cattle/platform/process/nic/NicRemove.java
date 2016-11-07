package io.cattle.platform.process.nic;

import io.cattle.platform.core.dao.GenericMapDao;
import io.cattle.platform.core.model.IpAddress;
import io.cattle.platform.core.model.IpAddressNicMap;
import io.cattle.platform.core.model.Nic;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.process.base.AbstractDefaultProcessHandler;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class NicRemove extends AbstractDefaultProcessHandler {

    @Inject
    GenericMapDao mapDao;

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        /* Release requested IPs on delete, not on purge */
        Nic nic = (Nic) state.getResource();

        for (IpAddressNicMap map : mapDao.findToRemove(IpAddressNicMap.class, Nic.class, nic.getId())) {
            IpAddress ipAddress = getObjectManager().loadResource(IpAddress.class, map.getIpAddressId());

            /* Deactivate to release the IP address */
            deactivate(ipAddress, state.getData());
        }

        return null;
    }

}
