package io.cattle.platform.agent.instance.service.impl;

import static io.cattle.platform.core.model.tables.InstanceTable.INSTANCE;
import static io.cattle.platform.core.model.tables.NicTable.NIC;
import static io.cattle.platform.core.model.tables.VnetTable.VNET;
import io.cattle.platform.core.model.Nic;
import io.cattle.platform.core.model.tables.records.NicRecord;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;

import java.util.List;

public class NicPerVnetNicLookup extends AbstractJooqDao {

    public List<? extends Nic> getNicPerVnetForAccount(long accountId) {
        return create()
                .select(NIC.fields())
                .from(NIC)
                .join(VNET)
                .on(VNET.ID.eq(NIC.VNET_ID))
                .join(INSTANCE)
                .on(INSTANCE.ID.eq(NIC.INSTANCE_ID))
                .where(NIC.ACCOUNT_ID.eq(accountId))
                .and(NIC.REMOVED.isNull())
                .and(INSTANCE.REMOVED.isNull())
                .and(INSTANCE.SYSTEM_CONTAINER.isNull())
                .groupBy(VNET.ID)
                .fetchInto(NicRecord.class);
    }
}
