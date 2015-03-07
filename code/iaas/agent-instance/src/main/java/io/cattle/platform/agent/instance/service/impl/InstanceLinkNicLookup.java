package io.cattle.platform.agent.instance.service.impl;

import static io.cattle.platform.core.model.tables.InstanceLinkTable.*;
import static io.cattle.platform.core.model.tables.NicTable.*;
import io.cattle.platform.agent.instance.service.InstanceNicLookup;
import io.cattle.platform.core.model.InstanceLink;
import io.cattle.platform.core.model.Nic;
import io.cattle.platform.core.model.tables.records.NicRecord;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;

import java.util.Arrays;
import java.util.List;

public class InstanceLinkNicLookup extends AbstractJooqDao implements InstanceNicLookup {

    @Override
    public List<Nic> getNics(Object obj) {
        Nic nic = null;
        if (obj instanceof InstanceLink) {
            InstanceLink link = (InstanceLink) obj;

            nic = create().select(NIC.fields()).from(INSTANCE_LINK).join(NIC).on(NIC.INSTANCE_ID.eq(INSTANCE_LINK.INSTANCE_ID)).where(
                    NIC.DEVICE_NUMBER.eq(0).and(INSTANCE_LINK.ID.eq(link.getId()))).fetchOneInto(NicRecord.class);
        }

        return nic == null ? null : Arrays.asList(nic);
    }

}
