package io.cattle.platform.agent.instance.service.impl;

import static io.cattle.platform.core.model.tables.InstanceTable.INSTANCE;
import static io.cattle.platform.core.model.tables.NicTable.NIC;
import static io.cattle.platform.core.model.tables.VnetTable.VNET;
import static io.cattle.platform.core.model.tables.HostVnetMapTable.*;
import static io.cattle.platform.core.model.tables.HostTable.*;
import io.cattle.platform.core.model.Nic;
import io.cattle.platform.core.model.Vnet;
import io.cattle.platform.core.model.tables.records.NicRecord;
import io.cattle.platform.core.model.tables.records.VnetRecord;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;

import java.util.ArrayList;
import java.util.List;

public class NicPerVnetNicLookup extends AbstractJooqDao {

    public List<? extends Nic> getNicPerVnetForAccount(long accountId) {
        List<Nic> nics = new ArrayList<>();
        // join on host here because vnet and host_vnet_map
        // are not removed on host removal - something to fix
        List<? extends Vnet> vnets = create()
                .select(VNET.fields())
                .from(VNET)
                .join(HOST_VNET_MAP)
                .on(HOST_VNET_MAP.VNET_ID.eq(VNET.ID))
                .join(HOST)
                .on(HOST.ID.eq(HOST_VNET_MAP.HOST_ID))
                .where(VNET.REMOVED.isNull())
                .and(HOST.REMOVED.isNull())
                .and(VNET.ACCOUNT_ID.eq(accountId))
                .fetchInto(VnetRecord.class);
        
        // for each vnet, get one user container's nic
        for (Vnet vnet : vnets) {
            nics.addAll(create()
                    .select(NIC.fields())
                    .from(NIC)
                    .join(INSTANCE)
                    .on(INSTANCE.ID.eq(NIC.INSTANCE_ID))
                    .where(NIC.REMOVED.isNull())
                    .and(INSTANCE.REMOVED.isNull())
                    .and(INSTANCE.SYSTEM_CONTAINER.isNull())
                    .and(NIC.VNET_ID.eq(vnet.getId())).limit(1)
                    .fetchInto(NicRecord.class));
        }
        return nics;
    }
}
