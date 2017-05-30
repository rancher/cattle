package io.cattle.platform.core.dao.impl;

import static io.cattle.platform.core.model.tables.AccountTable.*;
import static io.cattle.platform.core.model.tables.IpAddressTable.*;
import static io.cattle.platform.core.model.tables.NetworkDriverTable.*;
import static io.cattle.platform.core.model.tables.NetworkTable.*;
import static io.cattle.platform.core.model.tables.NicTable.*;
import static io.cattle.platform.core.model.tables.ServiceExposeMapTable.*;
import static io.cattle.platform.core.model.tables.SubnetTable.*;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.constants.AccountConstants;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.PortConstants;
import io.cattle.platform.core.constants.SubnetConstants;
import io.cattle.platform.core.dao.AccountDao;
import io.cattle.platform.core.dao.GenericResourceDao;
import io.cattle.platform.core.dao.NetworkDao;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Network;
import io.cattle.platform.core.model.Nic;
import io.cattle.platform.core.model.Port;
import io.cattle.platform.core.model.Subnet;
import io.cattle.platform.core.model.tables.records.NetworkRecord;
import io.cattle.platform.core.util.PortSpec;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.lock.LockCallback;
import io.cattle.platform.lock.LockManager;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.util.net.NetUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import com.netflix.config.DynamicStringListProperty;

@Named
public class NetworkDaoImpl extends AbstractJooqDao implements NetworkDao {
    DynamicStringListProperty DOCKER_VIP_SUBNET_CIDR = ArchaiusUtil.getList("docker.vip.subnet.cidr");

    @Inject
    ObjectManager objectManager;

    @Inject
    AccountDao accountDao;

    @Inject
    GenericResourceDao resourceDao;

    @Inject
    ObjectProcessManager objectProcessManager;

    @Inject
    LockManager lockManager;

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

    protected Network getInstancePrimaryNetwork(Instance instance) {
        Nic primaryNic = getPrimaryNic(instance.getId());
        return objectManager.loadResource(Network.class, primaryNic.getNetworkId());
    }

    @Override
    public Subnet addVIPSubnet(final long accountId) {
        return lockManager.lock(new SubnetCreateLock(accountId), new LockCallback<Subnet>() {
            @Override
            public Subnet doWithLock() {
                List<Subnet> subnets = objectManager.find(Subnet.class, SUBNET.ACCOUNT_ID, accountId, SUBNET.KIND,
                        SubnetConstants.KIND_VIP_SUBNET);
                if (subnets.size() > 0) {
                    return subnets.get(0);
                }

                Pair<String, Integer> cidr = NetUtils.getCidrAndSize(DOCKER_VIP_SUBNET_CIDR.get().get(0));

                return resourceDao.createAndSchedule(Subnet.class,
                        SUBNET.ACCOUNT_ID, accountId,
                        SUBNET.CIDR_SIZE, cidr.getRight(),
                        SUBNET.NETWORK_ADDRESS, cidr.getLeft(),
                        SUBNET.KIND, SubnetConstants.KIND_VIP_SUBNET);
            }
        });
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
            .select(SERVICE_EXPOSE_MAP.INSTANCE_ID)
            .from(SERVICE_EXPOSE_MAP)
            .where(SERVICE_EXPOSE_MAP.SERVICE_ID.eq(serviceId)
                    .and(SERVICE_EXPOSE_MAP.REMOVED.isNull()))
            .fetch().intoArray(SERVICE_EXPOSE_MAP.INSTANCE_ID);

        return create().select(NIC.INSTANCE_ID)
            .from(NIC)
            .join(NETWORK)
                .on(NIC.NETWORK_ID.eq(NETWORK.ID))
            .join(NETWORK_DRIVER)
                .on(NETWORK_DRIVER.ID.eq(NETWORK.NETWORK_DRIVER_ID))
            .where(NETWORK_DRIVER.SERVICE_ID.eq(serviceId)
                    .and(NIC.REMOVED.isNull())
                    .and(NIC.INSTANCE_ID.notIn(ignore)))
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

    @Override
    public void updateInstancePorts(Instance instance, List<String> newPortDefs, List<Port> toCreate,
            List<Port> toRemove, Map<String, Port> toRetain) {

        Map<String, Port> existingPorts = new HashMap<>();
        for (Port port : objectManager.children(instance, Port.class)) {
            if (port.getRemoved() != null || port.getState().equalsIgnoreCase(CommonStatesConstants.REMOVING)) {
                continue;
            }
            existingPorts.put(toKey(port), port);
        }

        for (String port : newPortDefs) {
            PortSpec spec = new PortSpec(port);

            if (existingPorts.containsKey(toKey(spec))) {
                toRetain.put(toKey(spec), existingPorts.get(toKey(spec)));
                continue;
            }

            Port portObj = objectManager.newRecord(Port.class);
            portObj.setAccountId(instance.getAccountId());
            portObj.setKind(PortConstants.KIND_USER);
            portObj.setInstanceId(instance.getId());
            portObj.setPublicPort(spec.getPublicPort());
            portObj.setPrivatePort(spec.getPrivatePort());
            portObj.setProtocol(spec.getProtocol());
            if (StringUtils.isNotEmpty(spec.getIpAddress())) {
                DataAccessor.fields(portObj).withKey(PortConstants.FIELD_BIND_ADDR).set(spec.getIpAddress());
            }
            toCreate.add(portObj);
            toRetain.put(toKey(portObj), portObj);
        }

        // remove extra ports
        for (String existing : existingPorts.keySet()) {
            if (!toRetain.containsKey(existing)) {
                toRemove.add(existingPorts.get(existing));
            }
        }
    }

    protected String toKey(PortSpec spec) {
        return String.format("%d:%d/%s", spec.getPublicPort(), spec.getPrivatePort(), spec.getProtocol());
    }

    protected String toKey(Port port) {
        return String.format("%d:%d/%s", port.getPublicPort(), port.getPrivatePort(), port.getProtocol());
    }

    @Override
    public void migrateToNetwork(Network network) {
        Network hostOnly = objectManager.findAny(Network.class,
                NETWORK.ACCOUNT_ID, network.getAccountId(),
                NETWORK.KIND, "hostOnlyNetwork");

        if (hostOnly != null) {
            create()
                .update(SUBNET)
                .set(SUBNET.NETWORK_ID, network.getId())
                .where(SUBNET.NETWORK_ID.eq(hostOnly.getId()))
                .execute();
            create()
                .update(IP_ADDRESS)
                .set(IP_ADDRESS.NETWORK_ID, network.getId())
                .where(IP_ADDRESS.NETWORK_ID.eq(hostOnly.getId()))
                .execute();
            create()
                .update(NIC)
                .set(NIC.NETWORK_ID, network.getId())
                .where(NIC.NETWORK_ID.eq(hostOnly.getId()))
                .execute();
        }

        create()
            .update(ACCOUNT)
            .set(ACCOUNT.DEFAULT_NETWORK_ID, network.getId())
            .where(ACCOUNT.ID.eq(network.getAccountId()))
            .execute();
    }

    @Override
    public List<? extends Network> findBadNetworks(int count) {
        return create().select(NETWORK.fields())
                .from(NETWORK)
                .join(ACCOUNT)
                    .on(ACCOUNT.ID.eq(NETWORK.ACCOUNT_ID))
                .where(NETWORK.REMOVED.isNull()
                        .and(ACCOUNT.STATE.eq(AccountConstants.STATE_PURGED))
                        .and(NETWORK.STATE.notIn(CommonStatesConstants.REMOVING)))
                .limit(count)
                .fetchInto(NetworkRecord.class);
    }

}
