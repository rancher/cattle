package io.cattle.platform.agent.instance.service.impl;

import static io.cattle.platform.core.model.tables.HealthcheckInstanceHostMapTable.HEALTHCHECK_INSTANCE_HOST_MAP;
import static io.cattle.platform.core.model.tables.HealthcheckInstanceTable.HEALTHCHECK_INSTANCE;
import static io.cattle.platform.core.model.tables.NicTable.NIC;
import io.cattle.platform.agent.instance.service.InstanceNicLookup;
import io.cattle.platform.core.model.HealthcheckInstanceHostMap;
import io.cattle.platform.core.model.Nic;
import io.cattle.platform.core.model.tables.records.NicRecord;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;

import java.util.List;

public class HealthcheckInstanceHostMapNicLookup extends AbstractJooqDao implements InstanceNicLookup {

    @Override
    public List<? extends Nic> getNics(Object obj) {
        if (!(obj instanceof HealthcheckInstanceHostMap)) {
            return null;
        }
        HealthcheckInstanceHostMap hostMap = (HealthcheckInstanceHostMap) obj;
        return create()
                .select(NIC.fields())
                .from(NIC)
                .join(HEALTHCHECK_INSTANCE)
                .on(HEALTHCHECK_INSTANCE.INSTANCE_ID.eq(NIC.INSTANCE_ID))
                .join(HEALTHCHECK_INSTANCE_HOST_MAP)
                .on(HEALTHCHECK_INSTANCE_HOST_MAP.HEALTHCHECK_INSTANCE_ID.eq(HEALTHCHECK_INSTANCE.ID))
                .where(HEALTHCHECK_INSTANCE_HOST_MAP.HOST_ID.eq(hostMap.getHostId())
                        .and(NIC.REMOVED.isNull())
                        .and(HEALTHCHECK_INSTANCE.REMOVED.isNull())).limit(1)
                .fetchInto(NicRecord.class);
    }
}
