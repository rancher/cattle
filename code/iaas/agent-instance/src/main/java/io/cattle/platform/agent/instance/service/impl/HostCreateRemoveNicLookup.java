package io.cattle.platform.agent.instance.service.impl;

import static io.cattle.platform.core.model.tables.InstanceHostMapTable.INSTANCE_HOST_MAP;
import static io.cattle.platform.core.model.tables.InstanceTable.INSTANCE;
import static io.cattle.platform.core.model.tables.NetworkTable.NETWORK;
import static io.cattle.platform.core.model.tables.NicTable.NIC;
import io.cattle.platform.agent.instance.service.InstanceNicLookup;
import io.cattle.platform.core.constants.NetworkConstants;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Network;
import io.cattle.platform.core.model.Nic;
import io.cattle.platform.core.model.tables.records.NicRecord;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.object.ObjectManager;

import java.util.List;

import javax.inject.Inject;

/*
 * This class is used by metadata service to 
 * push the update to all network agents on host.add event
 */
public class HostCreateRemoveNicLookup extends AbstractJooqDao implements InstanceNicLookup {

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

        // need to get one instance per host
        return create().
                select(NIC.fields()).
                from(NIC)
                .join(INSTANCE)
                .on(INSTANCE.ID.eq(NIC.INSTANCE_ID))
                .join(INSTANCE_HOST_MAP)
                .on(INSTANCE_HOST_MAP.INSTANCE_ID.eq(INSTANCE.ID))
                .where(NIC.REMOVED.isNull())
                .and(INSTANCE.REMOVED.isNull())
                .and(INSTANCE_HOST_MAP.REMOVED.isNull())
                .and(NIC.ACCOUNT_ID.eq(host.getAccountId()))
                .and(NIC.NETWORK_ID.eq(managedNtwk.getId()))
                .and(INSTANCE.SYSTEM_CONTAINER.isNull()).groupBy(INSTANCE_HOST_MAP.HOST_ID)
                .fetchInto(NicRecord.class);
    }
}
