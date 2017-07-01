package io.cattle.platform.core.dao.impl;

import static io.cattle.platform.core.model.tables.InstanceTable.*;
import static io.cattle.platform.core.model.tables.NetworkDriverTable.*;
import static io.cattle.platform.core.model.tables.NetworkTable.*;
import static io.cattle.platform.core.model.tables.SubnetTable.*;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.dao.GenericResourceDao;
import io.cattle.platform.core.dao.NetworkDao;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.Network;
import io.cattle.platform.core.model.Subnet;
import io.cattle.platform.core.model.tables.records.NetworkRecord;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.lock.LockManager;
import io.cattle.platform.object.ObjectManager;

import java.util.List;

import org.jooq.Configuration;

import com.netflix.config.DynamicStringListProperty;

public class NetworkDaoImpl extends AbstractJooqDao implements NetworkDao {
    DynamicStringListProperty DOCKER_VIP_SUBNET_CIDR = ArchaiusUtil.getList("docker.vip.subnet.cidr");

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
        return objectManager.findAny(Network.class,
                NETWORK.KIND, kind,
                NETWORK.ACCOUNT_ID, accountId,
                NETWORK.REMOVED, null);
    }

    @Override
    public Network getNetworkByName(long accountId, String name) {
        return objectManager.findAny(Network.class,
                NETWORK.NAME, name,
                NETWORK.ACCOUNT_ID, accountId,
                NETWORK.REMOVED, null);
    }

    @Override
    public Network getDefaultNetwork(Long accountId) {
        Account account = objectManager.loadResource(Account.class, accountId);
        if (account == null) {
            return null;
        }
        return objectManager.loadResource(Network.class, account.getDefaultNetworkId());
    }

    @Override
    public List<Long> findInstancesInUseByServiceDriver(Long serviceId) {
        Long[] ignore = create()
            .select(INSTANCE.ID)
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
    public List<Subnet> getSubnets(Network network) {
        return objectManager.find(Subnet.class,
                SUBNET.NETWORK_ID, network.getId(),
                SUBNET.STATE, CommonStatesConstants.ACTIVE);
    }

    @Override
    public List<? extends Network> getActiveNetworks(Long accountId) {
        return create().select(NETWORK.fields())
                .from(NETWORK)
                .where(NETWORK.ACCOUNT_ID.eq(accountId)
                    .and(NETWORK.STATE.in(CommonStatesConstants.ACTIVATING, CommonStatesConstants.ACTIVE, CommonStatesConstants.UPDATING_ACTIVE)))
                .fetchInto(NetworkRecord.class);
    }

}
