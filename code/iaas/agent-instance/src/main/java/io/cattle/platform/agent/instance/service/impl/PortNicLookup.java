package io.cattle.platform.agent.instance.service.impl;

import java.util.Collections;
import java.util.List;

import static io.cattle.platform.core.model.tables.NicTable.*;
import io.cattle.platform.agent.instance.service.InstanceNicLookup;
import io.cattle.platform.core.model.Nic;
import io.cattle.platform.core.model.Port;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;

public class PortNicLookup extends AbstractJooqDao implements InstanceNicLookup {

    @Override
    public List<? extends Nic> getNics(Object obj) {
        if (!(obj instanceof Port)) {
            return null;
        }

        Port port = (Port) obj;

        if (port.getInstanceId() == null) {
            return Collections.emptyList();
        }

        return create().selectFrom(NIC).where(NIC.INSTANCE_ID.eq(port.getInstanceId())).fetch();
    }

}
