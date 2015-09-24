package io.cattle.platform.process.nic;

import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.dao.GenericMapDao;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.IpAddress;
import io.cattle.platform.core.model.IpAddressNicMap;
import io.cattle.platform.core.model.Nic;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.process.base.AbstractDefaultProcessHandler;
import io.cattle.platform.resource.pool.ResourcePoolManager;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class NicDeactivate extends AbstractDefaultProcessHandler {

    GenericMapDao mapDao;
    ResourcePoolManager poolManager;

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        /* Release requested IPs on delete, not on purge */
        Nic nic = (Nic) state.getResource();
        Instance instance = objectManager.loadResource(Instance.class, nic.getInstanceId());
        if (instance == null) {
            return null;
        }

        String ip = DataAccessor.fieldString(instance, InstanceConstants.FIELD_REQUESTED_IP_ADDRESS);
        if (ip == null) {
            return null;
        }

        for (IpAddressNicMap map : mapDao.findToRemove(IpAddressNicMap.class, Nic.class, nic.getId())) {
            IpAddress ipAddress = getObjectManager().loadResource(IpAddress.class, map.getIpAddressId());

            if (!ip.equals(ipAddress.getAddress())) {
                continue;
            }

            /* Deactivate to release the IP address */
            deactivate(ipAddress, state.getData());
        }

        return null;
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
