package io.cattle.platform.core.dao.impl;

import static io.cattle.platform.core.model.tables.NetworkServiceTable.*;
import static io.cattle.platform.core.model.tables.NetworkServiceProviderTable.*;
import static io.cattle.platform.core.model.tables.NicTable.*;
import io.cattle.platform.core.constants.NetworkServiceProviderConstants;
import io.cattle.platform.core.dao.NetworkDao;
import io.cattle.platform.core.model.NetworkService;
import io.cattle.platform.core.model.Nic;
import io.cattle.platform.core.model.tables.records.NetworkServiceRecord;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;

import java.util.List;

public class NetworkDaoImpl extends AbstractJooqDao implements NetworkDao {

    @Override
    public List<? extends NetworkService> getAgentInstanceNetworkService(long instanceId, String serviceKind) {
        return create()
                .select(NETWORK_SERVICE.fields())
                .from(NETWORK_SERVICE)
                .join(NETWORK_SERVICE_PROVIDER)
                    .on(NETWORK_SERVICE_PROVIDER.ID.eq(NETWORK_SERVICE.NETWORK_SERVICE_PROVIDER_ID))
                .join(NIC)
                    .on(NIC.NETWORK_ID.eq(NETWORK_SERVICE.NETWORK_ID))
                .where(NIC.INSTANCE_ID.eq(instanceId)
                        .and(NETWORK_SERVICE_PROVIDER.KIND.eq(NetworkServiceProviderConstants.KIND_AGENT_INSTANCE))
                        .and(NETWORK_SERVICE.KIND.eq(serviceKind))
                        .and(NETWORK_SERVICE.REMOVED.isNull()))
                .fetchInto(NetworkServiceRecord.class);
    }

    @Override
    public List<? extends NetworkService> getNetworkService(long instanceId, String serviceKind) {
        return create()
                .select(NETWORK_SERVICE.fields())
                .from(NETWORK_SERVICE)
                .join(NETWORK_SERVICE_PROVIDER)
                    .on(NETWORK_SERVICE_PROVIDER.ID.eq(NETWORK_SERVICE.NETWORK_SERVICE_PROVIDER_ID))
                .join(NIC)
                    .on(NIC.NETWORK_ID.eq(NETWORK_SERVICE.NETWORK_ID))
                .where(NIC.INSTANCE_ID.eq(instanceId)
                        .and(NETWORK_SERVICE.KIND.eq(serviceKind))
                        .and(NETWORK_SERVICE.REMOVED.isNull()))
                .fetchInto(NetworkServiceRecord.class);
    }

    @Override
    public Nic getPrimaryNic(long instanceId) {
        return create()
                .selectFrom(NIC)
                .where(NIC.INSTANCE_ID.eq(instanceId)
                        .and(NIC.DEVICE_NUMBER.eq(0))
                        .and(NIC.REMOVED.isNull()))
                .fetchAny();
    }

}
