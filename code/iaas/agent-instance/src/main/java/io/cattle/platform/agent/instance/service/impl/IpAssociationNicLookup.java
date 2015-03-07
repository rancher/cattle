package io.cattle.platform.agent.instance.service.impl;

import static io.cattle.platform.core.model.tables.InstanceTable.*;
import static io.cattle.platform.core.model.tables.IpAddressNicMapTable.*;
import static io.cattle.platform.core.model.tables.NicTable.*;
import io.cattle.platform.agent.instance.service.InstanceNicLookup;
import io.cattle.platform.core.model.IpAssociation;
import io.cattle.platform.core.model.Nic;
import io.cattle.platform.core.model.tables.records.NicRecord;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;

import java.util.List;

public class IpAssociationNicLookup extends AbstractJooqDao implements InstanceNicLookup {

    @Override
    public List<? extends Nic> getNics(Object obj) {
        if (!(obj instanceof IpAssociation)) {
            return null;
        }

        IpAssociation assoc = (IpAssociation) obj;

        return create().select(NIC.fields()).from(NIC).join(INSTANCE).on(INSTANCE.ID.eq(NIC.INSTANCE_ID)).join(IP_ADDRESS_NIC_MAP).on(
                IP_ADDRESS_NIC_MAP.NIC_ID.eq(NIC.ID)).where(IP_ADDRESS_NIC_MAP.IP_ADDRESS_ID.eq(assoc.getChildIpAddressId())).fetchInto(NicRecord.class);

    }

}
