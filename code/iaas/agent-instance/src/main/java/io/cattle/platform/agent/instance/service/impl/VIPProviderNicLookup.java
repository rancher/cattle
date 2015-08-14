package io.cattle.platform.agent.instance.service.impl;

import static io.cattle.platform.core.model.tables.InstanceHostMapTable.INSTANCE_HOST_MAP;
import static io.cattle.platform.core.model.tables.InstanceTable.INSTANCE;
import static io.cattle.platform.core.model.tables.NetworkServiceProviderInstanceMapTable.NETWORK_SERVICE_PROVIDER_INSTANCE_MAP;
import static io.cattle.platform.core.model.tables.NetworkServiceTable.NETWORK_SERVICE;
import static io.cattle.platform.core.model.tables.NicTable.NIC;
import io.cattle.platform.agent.instance.service.InstanceNicLookup;
import io.cattle.platform.core.constants.NetworkServiceConstants;
import io.cattle.platform.core.model.InstanceHostMap;
import io.cattle.platform.core.model.NetworkServiceProviderInstanceMap;
import io.cattle.platform.core.model.Nic;
import io.cattle.platform.core.model.tables.records.InstanceHostMapRecord;
import io.cattle.platform.core.model.tables.records.NicRecord;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;

import java.util.List;

public class VIPProviderNicLookup extends AbstractJooqDao implements InstanceNicLookup {
    @Override
    public List<? extends Nic> getNics(Object obj) {
        if (!(obj instanceof NetworkServiceProviderInstanceMap)) {
            return null;
        }

        NetworkServiceProviderInstanceMap instanceMap = (NetworkServiceProviderInstanceMap) obj;
        List<? extends InstanceHostMap> hostMaps = create()
                .select(INSTANCE_HOST_MAP.fields())
                .from(INSTANCE_HOST_MAP)
                .join(NETWORK_SERVICE_PROVIDER_INSTANCE_MAP)
                .on(NETWORK_SERVICE_PROVIDER_INSTANCE_MAP.INSTANCE_ID.eq(INSTANCE_HOST_MAP.INSTANCE_ID))
                .join(NETWORK_SERVICE)
                .on(NETWORK_SERVICE.NETWORK_SERVICE_PROVIDER_ID
                        .eq(NETWORK_SERVICE_PROVIDER_INSTANCE_MAP.NETWORK_SERVICE_PROVIDER_ID))
                .where(NETWORK_SERVICE.KIND.eq(NetworkServiceConstants.KIND_VIP)
                        .and(NETWORK_SERVICE_PROVIDER_INSTANCE_MAP.ID.eq(instanceMap.getId())))
                .fetchInto(InstanceHostMapRecord.class);

        if (hostMaps == null || hostMaps.isEmpty()) {
            return null;
        }

        long hostId = hostMaps.get(0).getHostId();
        return create()
                .select(NIC.fields())
                .from(NIC)
                .join(INSTANCE)
                .on(INSTANCE.ID.eq(NIC.INSTANCE_ID))
                .join(INSTANCE_HOST_MAP)
                .on(INSTANCE_HOST_MAP.INSTANCE_ID.eq(INSTANCE.ID))
                .where(INSTANCE_HOST_MAP.HOST_ID.eq(hostId)
                        .and(NIC.REMOVED.isNull())
                        .and(INSTANCE.REMOVED.isNull()))
                .fetchInto(NicRecord.class);
    }
}
