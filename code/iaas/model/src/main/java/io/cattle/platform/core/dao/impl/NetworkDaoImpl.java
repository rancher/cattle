package io.cattle.platform.core.dao.impl;



import static io.cattle.platform.core.model.tables.HostIpAddressMapTable.*;
import static io.cattle.platform.core.model.tables.InstanceHostMapTable.*;
import static io.cattle.platform.core.model.tables.IpAddressTable.*;
import static io.cattle.platform.core.model.tables.NetworkServiceProviderInstanceMapTable.*;
import static io.cattle.platform.core.model.tables.NetworkServiceProviderTable.*;
import static io.cattle.platform.core.model.tables.NetworkServiceTable.*;
import static io.cattle.platform.core.model.tables.NetworkTable.*;
import static io.cattle.platform.core.model.tables.NicTable.*;
import static io.cattle.platform.core.model.tables.SubnetTable.*;
import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.constants.NetworkServiceProviderConstants;
import io.cattle.platform.core.constants.SubnetConstants;
import io.cattle.platform.core.dao.AccountDao;
import io.cattle.platform.core.dao.GenericResourceDao;
import io.cattle.platform.core.dao.NetworkDao;
import io.cattle.platform.core.model.InstanceHostMap;
import io.cattle.platform.core.model.IpAddress;
import io.cattle.platform.core.model.Network;
import io.cattle.platform.core.model.NetworkService;
import io.cattle.platform.core.model.NetworkServiceProvider;
import io.cattle.platform.core.model.Nic;
import io.cattle.platform.core.model.Subnet;
import io.cattle.platform.core.model.tables.InstanceHostMapTable;
import io.cattle.platform.core.model.tables.IpAddressTable;
import io.cattle.platform.core.model.tables.records.NetworkRecord;
import io.cattle.platform.core.model.tables.records.NetworkServiceRecord;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.db.jooq.mapper.MultiRecordMapper;
import io.cattle.platform.lock.LockCallback;
import io.cattle.platform.lock.LockManager;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.object.util.ObjectUtils;
import io.cattle.platform.util.net.NetUtils;
import io.cattle.platform.util.type.CollectionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jooq.Condition;
import org.jooq.impl.DSL;

import com.netflix.config.DynamicStringListProperty;

public class NetworkDaoImpl extends AbstractJooqDao implements NetworkDao {
    DynamicStringListProperty DOCKER_NETWORK_SUBNET_CIDR = ArchaiusUtil.getList("docker.network.subnet.cidr");
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
        Condition kindCond = DSL.trueCondition();
        if (kind != null) {
            kindCond = NETWORK.KIND.equalIgnoreCase(kind);
        }

        return create()
                .select(NETWORK.fields())
                .from(NETWORK)
                .where(NETWORK.ACCOUNT_ID.eq(accountId)
                        .and(kindCond)
                        .and(NETWORK.REMOVED.isNull()))
                .fetchInto(NetworkRecord.class);
    }

    @Override
    public Network getNetworkForObject(Object object, String networkKind) {
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

        List<? extends Network> accountNetworks = getNetworksForAccount(accountId, networkKind);

        if (!accountNetworks.isEmpty()) {
            return accountNetworks.get(0);

        }

        // TODO: remove
        // pass system network if account doesn't own any
        List<? extends Network> systemNetworks = getNetworksForAccount(accountDao.getSystemAccount()
                .getId(), networkKind);
        if (systemNetworks.isEmpty()) {
            return null;
        }
        return systemNetworks.get(0);
    }

    @Override
    public Subnet addManagedNetworkSubnet(Network network) {
        List<Subnet> subnets = objectManager.children(network, Subnet.class);
        if (subnets.size() > 0) {
            return subnets.get(0);
        }

        Pair<String, Integer> cidr = NetUtils.getCidrAndSize(DOCKER_NETWORK_SUBNET_CIDR.get().get(0));

        return resourceDao.createAndSchedule(Subnet.class,
                SUBNET.ACCOUNT_ID, network.getAccountId(),
                SUBNET.CIDR_SIZE, cidr.getRight(),
                SUBNET.NETWORK_ADDRESS, cidr.getLeft(),
                SUBNET.NETWORK_ID, network.getId());
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

    protected NetworkServiceProvider addNsp(Network network, String providerKind) {
        List<NetworkServiceProvider> nsps = objectManager.children(network, NetworkServiceProvider.class);

        for (NetworkServiceProvider nsp : nsps) {
            if (nsp.getKind().equalsIgnoreCase(providerKind)) {
                return nsp;
            }
        }

        return resourceDao.createAndSchedule(NetworkServiceProvider.class,
                NETWORK_SERVICE_PROVIDER.ACCOUNT_ID, network.getAccountId(),
                NETWORK_SERVICE_PROVIDER.KIND, providerKind,
                NETWORK_SERVICE_PROVIDER.NETWORK_ID, network.getId());
    }

    protected void addService(Map<String, NetworkService> services, NetworkServiceProvider nsp, String kind,
            Object... keyValue) {
        if (services.containsKey(kind)) {
            return;
        }

        Map<Object, Object> data = new HashMap<>();
        if (keyValue != null && keyValue.length > 1) {
            data = CollectionUtils.asMap(keyValue[0], ArrayUtils.subarray(keyValue, 1, keyValue.length));
        }

        data.put(NETWORK_SERVICE.KIND, kind);
        data.put(NETWORK_SERVICE.ACCOUNT_ID, nsp.getAccountId());
        data.put(NETWORK_SERVICE.NETWORK_ID, nsp.getNetworkId());
        data.put(NETWORK_SERVICE.NETWORK_SERVICE_PROVIDER_ID, nsp.getId());

        resourceDao.createAndSchedule(NetworkService.class,
                objectManager.convertToPropertiesFor(NetworkService.class, data));
    }

    @Override
    public NetworkServiceProvider createNsp(Network network, List<String> servicesKinds, String providerKind) {
        NetworkServiceProvider nsp = addNsp(network, providerKind);
        Map<String, NetworkService> initialServices = collectionNetworkServices(network);
        for (String serviceKind : servicesKinds) {
            addService(initialServices, nsp, serviceKind);
        }
        return nsp;
    }

    protected Map<String, NetworkService> collectionNetworkServices(Network network) {
        Map<String, NetworkService> services = new HashMap<>();

        for (NetworkService service : objectManager.children(network, NetworkService.class)) {
            services.put(service.getKind(), service);
        }

        return services;
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
}
