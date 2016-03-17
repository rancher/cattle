package io.cattle.platform.agent.instance.service.impl;

import static io.cattle.platform.core.model.tables.NetworkTable.NETWORK;
import io.cattle.platform.agent.instance.service.InstanceNicLookup;
import io.cattle.platform.core.constants.NetworkConstants;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Network;
import io.cattle.platform.core.model.Nic;
import io.cattle.platform.object.ObjectManager;

import java.util.List;

import javax.inject.Inject;

/*
 * This class is used by metadata service to 
 * push the update to all network agents on host.add event
 */
public class HostCreateRemoveNicLookup extends NicPerVnetNicLookup implements InstanceNicLookup {

    @Inject
    ObjectManager objMgr;

    @Override
    public List<? extends Nic> getNics(Object obj) {
        if (!(obj instanceof Host)) {
            return null;
        }

        Host host = (Host) obj;
        Network managedNtwk = objMgr.findAny(Network.class, NETWORK.ACCOUNT_ID, host.getAccountId(),
                NETWORK.REMOVED, null, NETWORK.KIND, NetworkConstants.KIND_HOSTONLY);
        if (managedNtwk == null) {
            return null;
        }

        return super.getRandomNicForAccount(host.getAccountId());
    }
}
