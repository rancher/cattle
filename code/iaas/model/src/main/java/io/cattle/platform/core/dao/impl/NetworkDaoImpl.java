package io.cattle.platform.core.dao.impl;

import static io.cattle.platform.core.model.tables.HostIpAddressMapTable.*;
import static io.cattle.platform.core.model.tables.InstanceHostMapTable.*;
import static io.cattle.platform.core.model.tables.InstanceTable.*;
import static io.cattle.platform.core.model.tables.IpAddressTable.*;
import static io.cattle.platform.core.model.tables.NetworkDriverTable.*;
import static io.cattle.platform.core.model.tables.NetworkTable.*;
import static io.cattle.platform.core.model.tables.NicTable.*;
import static io.cattle.platform.core.model.tables.SubnetTable.*;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.SubnetConstants;
import io.cattle.platform.core.dao.AccountDao;
import io.cattle.platform.core.dao.GenericResourceDao;
import io.cattle.platform.core.dao.NetworkDao;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.InstanceHostMap;
import io.cattle.platform.core.model.IpAddress;
import io.cattle.platform.core.model.Network;
import io.cattle.platform.core.model.Nic;
import io.cattle.platform.core.model.Subnet;
import io.cattle.platform.core.model.tables.InstanceHostMapTable;
import io.cattle.platform.core.model.tables.IpAddressTable;
import io.cattle.platform.core.model.tables.records.NetworkRecord;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.db.jooq.mapper.MultiRecordMapper;
import io.cattle.platform.lock.LockCallback;
import io.cattle.platform.lock.LockManager;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.util.net.NetUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang3.tuple.Pair;

import com.netflix.config.DynamicStringListProperty;

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
    public Map<Long, IpAddress> getInstanceWithHostNetworkingToIpMap(long accountId) {
        List<HostInstanceIpData> data = getHostContainerIpData(accountId);
        Map<Long, IpAddress> instanceIdToHostIpMap = new HashMap<>();
        for (HostInstanceIpData entry : data) {
            instanceIdToHostIpMap.put(entry.getInstanceHostMap().getInstanceId(), entry.getIpAddress());
        }

        return instanceIdToHostIpMap;
    }

    protected List<HostInstanceIpData> getHostContainerIpData(long accountId) {
        Network hostNtwk = objectManager.findAny(Network.class, NETWORK.ACCOUNT_ID, accountId, NETWORK.REMOVED, null,
                NETWORK.KIND, "dockerHost");
        if (hostNtwk == null) {
            return new ArrayList<HostInstanceIpData>();
        }
        MultiRecordMapper<HostInstanceIpData> mapper = new MultiRecordMapper<HostInstanceIpData>() {
            @Override
            protected HostInstanceIpData map(List<Object> input) {
                HostInstanceIpData data = new HostInstanceIpData();
                data.setIpAddress((IpAddress) input.get(0));
                data.setInstanceHostMap((InstanceHostMap) input.get(1));
                return data;
            }
        };

        IpAddressTable ipAddress = mapper.add(IP_ADDRESS);
        InstanceHostMapTable instanceHostMap = mapper.add(INSTANCE_HOST_MAP);
        return create()
                .select(mapper.fields())
                .from(HOST_IP_ADDRESS_MAP)
                .join(instanceHostMap)
                .on(HOST_IP_ADDRESS_MAP.HOST_ID.eq(instanceHostMap.HOST_ID))
                .join(NIC)
                .on(NIC.INSTANCE_ID.eq(instanceHostMap.INSTANCE_ID))
                .join(ipAddress)
                .on(HOST_IP_ADDRESS_MAP.IP_ADDRESS_ID.eq(ipAddress.ID))
                .where(instanceHostMap.REMOVED.isNull())
                .and(NIC.REMOVED.isNull())
                .and(HOST_IP_ADDRESS_MAP.REMOVED.isNull())
                .and(ipAddress.REMOVED.isNull())
                .and(NIC.NETWORK_ID.eq(hostNtwk.getId()))
                .fetch().map(mapper);
    }

    public class HostInstanceIpData {
        InstanceHostMap instanceHostMap;
        IpAddress ipAddress;

        public IpAddress getIpAddress() {
            return ipAddress;
        }

        public void setIpAddress(IpAddress ipAddress) {
            this.ipAddress = ipAddress;
        }

        public InstanceHostMap getInstanceHostMap() {
            return instanceHostMap;
        }

        public void setInstanceHostMap(InstanceHostMap instanceHostMap) {
            this.instanceHostMap = instanceHostMap;
        }
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
        return create().select(INSTANCE.ID)
            .from(NIC)
            .join(NETWORK)
                .on(NIC.NETWORK_ID.eq(NETWORK.ID))
            .join(NETWORK_DRIVER)
                .on(NETWORK_DRIVER.ID.eq(NETWORK.NETWORK_DRIVER_ID))
            .where(NETWORK_DRIVER.SERVICE_ID.eq(serviceId))
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
                    .and(NETWORK.STATE.in(CommonStatesConstants.ACTIVATING, CommonStatesConstants.ACTIVE)))
                .fetchInto(NetworkRecord.class);
    }

}