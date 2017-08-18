package io.cattle.platform.core.dao.impl;

import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.dao.GenericResourceDao;
import io.cattle.platform.core.dao.NetworkDao;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.Cluster;
import io.cattle.platform.core.model.Network;
import io.cattle.platform.core.model.Subnet;
import io.cattle.platform.core.model.tables.records.NetworkRecord;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.lock.LockManager;
import io.cattle.platform.object.ObjectManager;
import org.jooq.Configuration;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import static io.cattle.platform.core.model.Tables.*;
import static io.cattle.platform.core.model.tables.InstanceTable.INSTANCE;
import static io.cattle.platform.core.model.tables.NetworkDriverTable.NETWORK_DRIVER;
import static io.cattle.platform.core.model.tables.NetworkTable.NETWORK;
import static io.cattle.platform.core.model.tables.SubnetTable.SUBNET;

public class NetworkDaoImpl extends AbstractJooqDao implements NetworkDao {
    ObjectManager objectManager;
    GenericResourceDao resourceDao;
    LockManager lockManager;

    public NetworkDaoImpl(Configuration configuration, ObjectManager objectManager, GenericResourceDao resourceDao, LockManager lockManager) {
        super(configuration);
        this.objectManager = objectManager;
        this.resourceDao = resourceDao;
        this.lockManager = lockManager;
    }

    @Override
    public Network getNetworkByKind(long accountId, String kind) {
        return create()
                .select(NETWORK.fields())
                .from(NETWORK)
                .join(ACCOUNT)
                    .on(NETWORK.CLUSTER_ID.eq(ACCOUNT.CLUSTER_ID))
                .where(ACCOUNT.ID.eq(accountId)
                        .and(NETWORK.KIND.eq(kind))
                        .and(NETWORK.REMOVED.isNull()))
                .fetchAnyInto(NetworkRecord.class);
    }

    @Override
    public Network getNetworkByName(long accountId, String name) {
        return create()
                .select(NETWORK.fields())
                .from(NETWORK)
                .join(ACCOUNT)
                    .on(NETWORK.CLUSTER_ID.eq(ACCOUNT.CLUSTER_ID))
                .where(ACCOUNT.ID.eq(accountId)
                        .and(NETWORK.NAME.eq(name))
                        .and(NETWORK.REMOVED.isNull()))
                .fetchAnyInto(NetworkRecord.class);
    }

    @Override
    public Network getDefaultNetwork(Long accountId) {
        Account account = objectManager.loadResource(Account.class, accountId);
        if (account == null) {
            return null;
        }

        Cluster cluster = objectManager.loadResource(Cluster.class, account.getClusterId());
        if (cluster == null) {
            return null;
        }

        return objectManager.loadResource(Network.class, cluster.getDefaultNetworkId());
    }

    @Override
    public List<Long> findInstancesInUseByServiceDriver(Long serviceId) {
        Long[] ignore = create()
            .select(INSTANCE.ID)
            .from(INSTANCE)
            .where(INSTANCE.SERVICE_ID.eq(serviceId)
                    .and(INSTANCE.REMOVED.isNull()))
            .fetch().intoArray(INSTANCE.ID);

        return create().select(INSTANCE.ID)
            .from(INSTANCE)
            .join(NETWORK)
                .on(INSTANCE.NETWORK_ID.eq(NETWORK.ID))
            .join(NETWORK_DRIVER)
                .on(NETWORK_DRIVER.ID.eq(NETWORK.NETWORK_DRIVER_ID))
            .where(NETWORK_DRIVER.SERVICE_ID.eq(serviceId)
                    .and(INSTANCE.REMOVED.isNull())
                    .and(INSTANCE.ID.notIn(ignore)))
            .fetchInto(Long.class);
    }

    @Override
    public Collection<? extends Network> getNetworks(Set<Long> networkIds) {
        return create().select(NETWORK.fields())
                .from(NETWORK)
                .where(NETWORK.ID.in(networkIds))
                .fetchInto(NetworkRecord.class);
    }

    @Override
    public List<Subnet> getSubnets(Network network) {
        return objectManager.find(Subnet.class,
                SUBNET.NETWORK_ID, network.getId(),
                SUBNET.STATE, CommonStatesConstants.ACTIVE);
    }

    @Override
    public List<? extends Network> getActiveNetworks(Long clusterId) {
        return create().select(NETWORK.fields())
                .from(NETWORK)
                .where(NETWORK.CLUSTER_ID.eq(clusterId)
                    .and(NETWORK.STATE.in(CommonStatesConstants.ACTIVATING, CommonStatesConstants.ACTIVE, CommonStatesConstants.UPDATING)))
                .fetchInto(NetworkRecord.class);
    }

}
