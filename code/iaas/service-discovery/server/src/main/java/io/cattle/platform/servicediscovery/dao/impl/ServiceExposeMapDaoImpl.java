package io.cattle.platform.servicediscovery.dao.impl;

import static io.cattle.platform.core.model.tables.InstanceTable.INSTANCE;
import static io.cattle.platform.core.model.tables.ServiceExposeMapTable.SERVICE_EXPOSE_MAP;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.tables.records.InstanceRecord;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.servicediscovery.api.dao.ServiceExposeMapDao;

import java.util.List;

public class ServiceExposeMapDaoImpl extends AbstractJooqDao implements ServiceExposeMapDao {
    @Override
    public List<? extends Instance> listNonRemovedInstancesForService(long serviceId) {
        return create()
                .select(INSTANCE.fields())
                .from(INSTANCE)
                .join(SERVICE_EXPOSE_MAP)
                .on(SERVICE_EXPOSE_MAP.INSTANCE_ID.eq(INSTANCE.ID)
                        .and(SERVICE_EXPOSE_MAP.SERVICE_ID.eq(serviceId))
                        .and(SERVICE_EXPOSE_MAP.STATE.in(CommonStatesConstants.ACTIVATING,
                                CommonStatesConstants.ACTIVE)))
                .where(INSTANCE.REMOVED.isNull())
                .fetchInto(InstanceRecord.class);
    }

    @Override
    public Instance getServiceInstance(long serviceId, String instanceName) {
        List<? extends Instance> instances = create()
                .select(INSTANCE.fields())
                .from(INSTANCE)
                .join(SERVICE_EXPOSE_MAP)
                .on(SERVICE_EXPOSE_MAP.INSTANCE_ID.eq(INSTANCE.ID)
                        .and(SERVICE_EXPOSE_MAP.SERVICE_ID.eq(serviceId))
                        .and(SERVICE_EXPOSE_MAP.STATE.in(CommonStatesConstants.ACTIVATING,
                                CommonStatesConstants.ACTIVE)))
                .where(INSTANCE.REMOVED.isNull().and(INSTANCE.NAME.eq(instanceName)))
                .fetchInto(InstanceRecord.class);
        if (instances.isEmpty()) {
            return null;
        }
        return instances.get(0);
    }
}
