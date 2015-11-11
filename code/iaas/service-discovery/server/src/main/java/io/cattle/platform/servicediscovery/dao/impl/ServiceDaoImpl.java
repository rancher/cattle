package io.cattle.platform.servicediscovery.dao.impl;

import static io.cattle.platform.core.model.tables.HealthcheckInstanceHostMapTable.HEALTHCHECK_INSTANCE_HOST_MAP;
import static io.cattle.platform.core.model.tables.HealthcheckInstanceTable.HEALTHCHECK_INSTANCE;
import static io.cattle.platform.core.model.tables.InstanceHostMapTable.INSTANCE_HOST_MAP;
import static io.cattle.platform.core.model.tables.InstanceTable.INSTANCE;
import static io.cattle.platform.core.model.tables.ServiceExposeMapTable.SERVICE_EXPOSE_MAP;
import static io.cattle.platform.core.model.tables.ServiceTable.SERVICE;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.tables.records.InstanceRecord;
import io.cattle.platform.core.model.tables.records.ServiceRecord;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.servicediscovery.api.dao.ServiceDao;

import java.util.List;

public class ServiceDaoImpl extends AbstractJooqDao implements ServiceDao {

    @Override
    public List<? extends Service> getServicesOnHost(long hostId) {
        return create().select(SERVICE.fields())
                .from(SERVICE)
                .join(SERVICE_EXPOSE_MAP)
                    .on(SERVICE_EXPOSE_MAP.SERVICE_ID.eq(SERVICE.ID))
                .join(INSTANCE_HOST_MAP)
                    .on(SERVICE_EXPOSE_MAP.INSTANCE_ID.eq(INSTANCE_HOST_MAP.INSTANCE_ID))
                .where(INSTANCE_HOST_MAP.HOST_ID.eq(hostId))
                .and(INSTANCE_HOST_MAP.REMOVED.isNull())
                .and(SERVICE_EXPOSE_MAP.REMOVED.isNull())
                .and(SERVICE.REMOVED.isNull())
                .fetchInto(ServiceRecord.class);
    }

    @Override
    public List<? extends Instance> getInstancesWithHealtcheckScheduledOnHost(long hostId) {
        return create().select(INSTANCE.fields())
                .from(INSTANCE)
                .join(HEALTHCHECK_INSTANCE)
                .on(HEALTHCHECK_INSTANCE.INSTANCE_ID.eq(INSTANCE.ID))
                .join(HEALTHCHECK_INSTANCE_HOST_MAP)
                .on(HEALTHCHECK_INSTANCE_HOST_MAP.HEALTHCHECK_INSTANCE_ID.eq(HEALTHCHECK_INSTANCE.ID))
                .where(HEALTHCHECK_INSTANCE_HOST_MAP.HOST_ID.eq(hostId))
                .and(HEALTHCHECK_INSTANCE_HOST_MAP.REMOVED.isNull())
                .and(HEALTHCHECK_INSTANCE.REMOVED.isNull())
                .and(INSTANCE.REMOVED.isNull())
                .fetchInto(InstanceRecord.class);
    }

    @Override
    public List<? extends Instance> getInstancesWithHealtcheckRunningOnHost(long hostId) {
        return create().select(INSTANCE.fields())
                .from(INSTANCE)
                .join(HEALTHCHECK_INSTANCE)
                .on(HEALTHCHECK_INSTANCE.INSTANCE_ID.eq(INSTANCE.ID))
                .join(INSTANCE_HOST_MAP)
                .on(INSTANCE_HOST_MAP.INSTANCE_ID.eq(INSTANCE.ID))
                .where(INSTANCE_HOST_MAP.HOST_ID.eq(hostId))
                .and(INSTANCE_HOST_MAP.REMOVED.isNull())
                .and(HEALTHCHECK_INSTANCE.REMOVED.isNull())
                .and(INSTANCE.REMOVED.isNull())
                .and(INSTANCE.STATE.in(InstanceConstants.STATE_STARTING, InstanceConstants.STATE_RUNNING))
                .fetchInto(InstanceRecord.class);
    }
}
