package io.cattle.platform.core.dao.impl;


import static io.cattle.platform.core.model.tables.NetworkServiceProviderInstanceMapTable.NETWORK_SERVICE_PROVIDER_INSTANCE_MAP;
import static io.cattle.platform.core.model.tables.NetworkServiceProviderTable.NETWORK_SERVICE_PROVIDER;
import static io.cattle.platform.core.model.tables.NetworkServiceTable.NETWORK_SERVICE;
import static io.cattle.platform.core.model.tables.NetworkTable.NETWORK;
import static io.cattle.platform.core.model.tables.NicTable.NIC;
import io.cattle.platform.core.constants.NetworkConstants;
import io.cattle.platform.core.constants.NetworkServiceProviderConstants;
import io.cattle.platform.core.dao.AccountDao;
import io.cattle.platform.core.dao.NetworkDao;
import io.cattle.platform.core.model.Network;
import io.cattle.platform.core.model.NetworkService;
import io.cattle.platform.core.model.Nic;
import io.cattle.platform.core.model.tables.records.NetworkRecord;
import io.cattle.platform.core.model.tables.records.NetworkServiceRecord;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.object.util.ObjectUtils;

import java.util.List;

import javax.inject.Inject;

public class NetworkDaoImpl extends AbstractJooqDao implements NetworkDao {

    @Inject
    ObjectManager objectManager;

    @Inject
    AccountDao accountDao;

    @Override
    public List<? extends NetworkService> getAgentInstanceNetworkService(long instanceId, String serviceKind) {
        return create()
                .select(NETWORK_SERVICE.fields())
                .from(NETWORK_SERVICE)
                .join(NETWORK_SERVICE_PROVIDER)
                    .on(NETWORK_SERVICE_PROVIDER.ID.eq(NETWORK_SERVICE.NETWORK_SERVICE_PROVIDER_ID))
                .join(NIC)
                    .on(NIC.NETWORK_ID.eq(NETWORK_SERVICE.NETWORK_ID))
                .join(NETWORK_SERVICE_PROVIDER_INSTANCE_MAP)
                .on(NETWORK_SERVICE_PROVIDER_INSTANCE_MAP.INSTANCE_ID.eq(instanceId))
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

    @Override
    public List<? extends Network> getNetworksForAccount(long accountId, String kind) {
        return create()
                .select(NETWORK.fields())
                .from(NETWORK)
                .where(NETWORK.ACCOUNT_ID.eq(accountId)
                        .and(NETWORK.KIND.equalIgnoreCase(kind))
                        .and(NETWORK.REMOVED.isNull()))
                .fetchInto(NetworkRecord.class);
    }

    @Override
    public Network getNetworkForObject(Object object) {
        Long networkId = DataAccessor
                .fields(object)
                .withKey("networkId")
                .as(Long.class);
        if (networkId != null) {
            return objectManager.loadResource(Network.class, networkId);
        }

        Long accountId = (Long) ObjectUtils.getAccountId(object);
        if (accountId == null) {
            return null;
        }

        List<? extends Network> accountNetworks = getNetworksForAccount(accountId,
                NetworkConstants.KIND_HOSTONLY);

        if (!accountNetworks.isEmpty()) {
            return accountNetworks.get(0);

        }

        // pass system network if account doesn't own any
        List<? extends Network> systemNetworks = getNetworksForAccount(accountDao.getSystemAccount()
                .getId(),
                NetworkConstants.KIND_HOSTONLY);
        if (systemNetworks.isEmpty()) {
            return null;
        }
        return systemNetworks.get(0);
    }
}
