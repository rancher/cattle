package io.cattle.iaas.healthcheck.service.impl;

import static io.cattle.platform.core.model.tables.HostTable.HOST;
import static io.cattle.platform.core.model.tables.InstanceHostMapTable.INSTANCE_HOST_MAP;
import io.cattle.iaas.healthcheck.service.HealthcheckHostLookup;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.NetworkServiceProviderInstanceMap;
import io.cattle.platform.core.model.tables.records.HostRecord;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.servicediscovery.api.dao.ServiceDao;

import java.util.List;

import javax.inject.Inject;

public class ProviderInstanceMapHealthcheckHostLookup extends AbstractJooqDao implements
        HealthcheckHostLookup {

    @Inject
    ObjectManager objectManager;

    @Inject
    ServiceDao serviceDao;

    @Override
    public Host getHost(Object obj) {
        if (!(obj instanceof NetworkServiceProviderInstanceMap)) {
            return null;
        }
        NetworkServiceProviderInstanceMap map = (NetworkServiceProviderInstanceMap) obj;
        List<? extends Host> hosts = create()
                .select(HOST.fields())
                .from(HOST)
                .join(INSTANCE_HOST_MAP)
                .on(INSTANCE_HOST_MAP.HOST_ID.eq(HOST.ID))
                .where(INSTANCE_HOST_MAP.INSTANCE_ID.eq(map.getInstanceId()))
                .and(INSTANCE_HOST_MAP.REMOVED.isNull())
                .and(HOST.REMOVED.isNull())
                .fetchInto(HostRecord.class);

        if (hosts.isEmpty()) {
            return null;
        }
        return hosts.get(0);
    }
}
