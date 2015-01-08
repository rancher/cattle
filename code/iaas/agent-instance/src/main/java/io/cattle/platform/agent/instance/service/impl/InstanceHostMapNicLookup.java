package io.cattle.platform.agent.instance.service.impl;

import static io.cattle.platform.core.model.tables.NicTable.*;
import io.cattle.platform.agent.instance.service.InstanceNicLookup;
import io.cattle.platform.core.model.InstanceHostMap;
import io.cattle.platform.core.model.Nic;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;

import java.util.Collections;
import java.util.List;

public class InstanceHostMapNicLookup extends AbstractJooqDao implements InstanceNicLookup {

    @Override
    public List<? extends Nic> getNics(Object obj) {
        if (!(obj instanceof InstanceHostMap)) {
            return null;
        }

        InstanceHostMap map = (InstanceHostMap) obj;

        if (map.getInstanceId() == null) {
            return Collections.emptyList();
        }

        return create().selectFrom(NIC).where(NIC.INSTANCE_ID.eq(map.getInstanceId())).fetch();
    }

}
