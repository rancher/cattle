package io.cattle.platform.agent.instance.service.impl;

import static io.cattle.platform.core.model.tables.HostTable.*;
import static io.cattle.platform.core.model.tables.InstanceHostMapTable.*;
import static io.cattle.platform.core.model.tables.NicTable.*;
import io.cattle.platform.agent.instance.service.InstanceNicLookup;
import io.cattle.platform.core.model.HostIpAddressMap;
import io.cattle.platform.core.model.Nic;
import io.cattle.platform.core.model.tables.records.NicRecord;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;

import java.util.List;

public class HostIpAddressMapNicLookup extends AbstractJooqDao implements InstanceNicLookup {

    @Override
    public List<? extends Nic> getNics(Object obj) {
        if ( ! ( obj instanceof HostIpAddressMap ) ) {
            return null;
        }

        return create()
                .select(NIC.fields())
                .from(HOST)
                .join(INSTANCE_HOST_MAP)
                    .on(INSTANCE_HOST_MAP.HOST_ID.eq(HOST.ID))
                .join(NIC)
                    .on(NIC.INSTANCE_ID.eq(INSTANCE_HOST_MAP.INSTANCE_ID))
                .where(HOST.ID.eq(((HostIpAddressMap)obj).getHostId())
                        .and(NIC.REMOVED.isNull())
                        .and(INSTANCE_HOST_MAP.REMOVED.isNull()))
                .fetchInto(NicRecord.class);
    }

}
