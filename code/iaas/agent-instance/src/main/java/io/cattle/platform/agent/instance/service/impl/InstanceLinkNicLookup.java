package io.cattle.platform.agent.instance.service.impl;

import static io.cattle.platform.core.model.tables.NicTable.*;
import static io.cattle.platform.core.model.tables.InstanceLinkTable.*;
import io.cattle.platform.agent.instance.service.AgentInstanceNicLookup;
import io.cattle.platform.core.model.InstanceLink;
import io.cattle.platform.core.model.Nic;
import io.cattle.platform.core.model.tables.records.NicRecord;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;

public class InstanceLinkNicLookup extends AbstractJooqDao implements AgentInstanceNicLookup {

    @Override
    public Nic getNic(Object obj) {
        if ( obj instanceof InstanceLink ) {
            InstanceLink link = (InstanceLink)obj;

            return create()
                    .select(NIC.fields())
                    .from(INSTANCE_LINK)
                    .join(NIC)
                        .on(NIC.INSTANCE_ID.eq(INSTANCE_LINK.INSTANCE_ID))
                    .where(NIC.DEVICE_NUMBER.eq(0)
                            .and(INSTANCE_LINK.ID.eq(link.getId())))
                    .fetchOneInto(NicRecord.class);
        }

        return null;
    }

}
