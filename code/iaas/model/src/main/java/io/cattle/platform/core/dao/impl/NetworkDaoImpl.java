package io.cattle.platform.core.dao.impl;



import static io.cattle.platform.core.model.tables.InstanceHostMapTable.INSTANCE_HOST_MAP;
import static io.cattle.platform.core.model.tables.InstanceTable.INSTANCE;
import static io.cattle.platform.core.model.tables.NetworkServiceProviderInstanceMapTable.NETWORK_SERVICE_PROVIDER_INSTANCE_MAP;
import static io.cattle.platform.core.model.tables.NetworkServiceProviderTable.NETWORK_SERVICE_PROVIDER;
import static io.cattle.platform.core.model.tables.NetworkServiceTable.NETWORK_SERVICE;
import static io.cattle.platform.core.model.tables.NetworkTable.NETWORK;
import static io.cattle.platform.core.model.tables.NicTable.NIC;
import static io.cattle.platform.core.model.tables.SubnetTable.SUBNET;
import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.NetworkServiceProviderConstants;
import io.cattle.platform.core.constants.SubnetConstants;
import io.cattle.platform.core.dao.AccountDao;
import io.cattle.platform.core.dao.GenericResourceDao;
import io.cattle.platform.core.dao.NetworkDao;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Network;
import io.cattle.platform.core.model.NetworkService;
import io.cattle.platform.core.model.NetworkServiceProvider;
import io.cattle.platform.core.model.NetworkServiceProviderInstanceMap;
import io.cattle.platform.core.model.Nic;
import io.cattle.platform.core.model.Subnet;
import io.cattle.platform.core.model.tables.records.InstanceRecord;
import io.cattle.platform.core.model.tables.records.NetworkRecord;
import io.cattle.platform.core.model.tables.records.NetworkServiceProviderRecord;
import io.cattle.platform.core.model.tables.records.NetworkServiceRecord;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.lock.LockCallback;
import io.cattle.platform.lock.LockManager;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.object.util.ObjectUtils;
import io.cattle.platform.util.net.NetUtils;
import io.cattle.platform.util.type.CollectionUtils;

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
    public void registerNspInstance(String providerKind, Instance instance, List<String> services) {
        Network network = getInstancePrimaryNetwork(instance);
        NetworkServiceProvider provider = getNetworkServiceProviderForInstance(network, providerKind, services,
                instance);
        if (provider == null) {
            // create provider on demand
            provider = createNsp(network, services, providerKind);
        }
        resourceDao.createAndSchedule(NetworkServiceProviderInstanceMap.class,
                    NETWORK_SERVICE_PROVIDER_INSTANCE_MAP.INSTANCE_ID, instance.getId(),
                    NETWORK_SERVICE_PROVIDER_INSTANCE_MAP.NETWORK_SERVICE_PROVIDER_ID, provider.getId());
    }

    protected Network getInstancePrimaryNetwork(Instance instance) {
        Nic primaryNic = getPrimaryNic(instance.getId());
        return objectManager.loadResource(Network.class, primaryNic.getNetworkId());
    }

    @Override
    public List<NetworkServiceProviderInstanceMap> findNspInstanceMaps(Instance instance) {
        return objectManager.find(NetworkServiceProviderInstanceMap.class,
                NETWORK_SERVICE_PROVIDER_INSTANCE_MAP.INSTANCE_ID, instance.getId(),
                NETWORK_SERVICE_PROVIDER_INSTANCE_MAP.REMOVED, null);
    }

    protected NetworkServiceProvider getNetworkServiceProviderForInstance(Network network, String providerKind,
            List<String> services, Instance instance) {
        return getNetworkServiceProvider(providerKind, network.getId(), services);
    }

    protected NetworkServiceProvider getNetworkServiceProvider(String providerKind, long networkId,
            List<String> services) {
        List<? extends NetworkServiceProvider> providers = create()
                .select(NETWORK_SERVICE_PROVIDER.fields())
                .from(NETWORK_SERVICE_PROVIDER)
                .join(NETWORK_SERVICE)
                .on(NETWORK_SERVICE_PROVIDER.ID.eq(NETWORK_SERVICE.NETWORK_SERVICE_PROVIDER_ID))
                .where(NETWORK_SERVICE.NETWORK_ID.eq(networkId)
                        .and(NETWORK_SERVICE.KIND.in(services))
                        .and(NETWORK_SERVICE_PROVIDER.KIND.eq(providerKind)))
                .fetchInto(NetworkServiceProviderRecord.class);
        
        return providers.isEmpty() ? null : providers.get(0);
    }

    @Override
    public Instance getServiceProviderInstanceOnHostForNetwork(long networkId, String serviceKind, long hostId) {
        List<? extends Instance> instances = create()
                .select(INSTANCE.fields())
                .from(INSTANCE)
                .join(NETWORK_SERVICE_PROVIDER_INSTANCE_MAP)
                .on(NETWORK_SERVICE_PROVIDER_INSTANCE_MAP.INSTANCE_ID.eq(INSTANCE.ID))
                .join(NETWORK_SERVICE)
                .on(NETWORK_SERVICE.NETWORK_SERVICE_PROVIDER_ID
                        .eq(NETWORK_SERVICE_PROVIDER_INSTANCE_MAP.NETWORK_SERVICE_PROVIDER_ID))
                .join(INSTANCE_HOST_MAP)
                .on(INSTANCE_HOST_MAP.INSTANCE_ID.eq(INSTANCE.ID))
                .where(NETWORK_SERVICE.NETWORK_ID.eq(networkId)
                        .and(NETWORK_SERVICE.KIND.eq(serviceKind))
                        .and(INSTANCE_HOST_MAP.HOST_ID.eq(hostId))
                        .and(INSTANCE.STATE.in(InstanceConstants.STATE_STARTING, InstanceConstants.STATE_RESTARTING,
                                InstanceConstants.STATE_RUNNING))
                        .and(NETWORK_SERVICE_PROVIDER_INSTANCE_MAP.REMOVED.isNull()))
                .fetchInto(InstanceRecord.class);

        return instances.isEmpty() ? null : instances.get(0);
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
    public Instance getServiceProviderInstanceOnHost(String serviceKind, long hostId) {
        List<? extends Instance> instances = create()
                .select(INSTANCE.fields())
                .from(INSTANCE)
                .join(NETWORK_SERVICE_PROVIDER_INSTANCE_MAP)
                .on(NETWORK_SERVICE_PROVIDER_INSTANCE_MAP.INSTANCE_ID.eq(INSTANCE.ID))
                .join(NETWORK_SERVICE)
                .on(NETWORK_SERVICE.NETWORK_SERVICE_PROVIDER_ID
                        .eq(NETWORK_SERVICE_PROVIDER_INSTANCE_MAP.NETWORK_SERVICE_PROVIDER_ID))
                .join(INSTANCE_HOST_MAP)
                .on(INSTANCE_HOST_MAP.INSTANCE_ID.eq(INSTANCE.ID))
                .where(NETWORK_SERVICE.KIND.eq(serviceKind)
                        .and(INSTANCE_HOST_MAP.HOST_ID.eq(hostId))
                        .and(INSTANCE.STATE.in(InstanceConstants.STATE_STARTING, InstanceConstants.STATE_RESTARTING,
                                InstanceConstants.STATE_RUNNING))
                        .and(NETWORK_SERVICE_PROVIDER_INSTANCE_MAP.REMOVED.isNull()))
                .fetchInto(InstanceRecord.class);

        return instances.isEmpty() ? null : instances.get(0);
    }

    @Override
    public String getVIPSubnetCidr() {
        return DOCKER_VIP_SUBNET_CIDR.get().get(0);
    }
}
