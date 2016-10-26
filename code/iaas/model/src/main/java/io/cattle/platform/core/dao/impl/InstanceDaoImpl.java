package io.cattle.platform.core.dao.impl;

import static io.cattle.platform.core.model.tables.HostTable.*;
import static io.cattle.platform.core.model.tables.InstanceHostMapTable.*;
import static io.cattle.platform.core.model.tables.InstanceTable.*;
import static io.cattle.platform.core.model.tables.IpAddressNicMapTable.*;
import static io.cattle.platform.core.model.tables.IpAddressTable.*;
import static io.cattle.platform.core.model.tables.NicTable.*;
import static io.cattle.platform.core.model.tables.PortTable.*;
import static io.cattle.platform.core.model.tables.ServiceExposeMapTable.*;
import static io.cattle.platform.core.model.tables.ServiceIndexTable.*;
import static io.cattle.platform.core.model.tables.ServiceTable.*;
import static io.cattle.platform.core.model.tables.StackTable.*;
import static io.cattle.platform.core.model.tables.SubnetTable.*;

import io.cattle.platform.core.addon.PublicEndpoint;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.IpAddressConstants;
import io.cattle.platform.core.constants.PortConstants;
import io.cattle.platform.core.dao.GenericMapDao;
import io.cattle.platform.core.dao.InstanceDao;
import io.cattle.platform.core.dao.NetworkDao;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.IpAddress;
import io.cattle.platform.core.model.Port;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.ServiceExposeMap;
import io.cattle.platform.core.model.ServiceIndex;
import io.cattle.platform.core.model.Subnet;
import io.cattle.platform.core.model.tables.HostTable;
import io.cattle.platform.core.model.tables.InstanceTable;
import io.cattle.platform.core.model.tables.IpAddressTable;
import io.cattle.platform.core.model.tables.PortTable;
import io.cattle.platform.core.model.tables.ServiceExposeMapTable;
import io.cattle.platform.core.model.tables.ServiceIndexTable;
import io.cattle.platform.core.model.tables.SubnetTable;
import io.cattle.platform.core.model.tables.records.HostRecord;
import io.cattle.platform.core.model.tables.records.InstanceRecord;
import io.cattle.platform.core.model.tables.records.ServiceRecord;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.db.jooq.mapper.MultiRecordMapper;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.object.util.DataUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.jooq.Condition;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

public class InstanceDaoImpl extends AbstractJooqDao implements InstanceDao {
    @Inject
    GenericMapDao mapDao;
    @Inject
    ObjectManager objectManager;
    @Inject
    NetworkDao ntwkDao;

    public static class IpAddressToServiceIndex {
        ServiceIndex index;
        IpAddress ipAddress;
        Subnet subnet;

        public ServiceIndex getIndex() {
            return index;
        }

        public void setIndex(ServiceIndex index) {
            this.index = index;
        }

        public IpAddress getIpAddress() {
            return ipAddress;
        }

        public void setIpAddress(IpAddress ipAddress) {
            this.ipAddress = ipAddress;
        }

        public Subnet getSubnet() {
            return subnet;
        }

        public void setSubnet(Subnet subnet) {
            this.subnet = subnet;
        }

        public IpAddressToServiceIndex(ServiceIndex index, IpAddress ipAddress, Subnet subnet) {
            super();
            this.index = index;
            this.ipAddress = ipAddress;
            this.subnet = subnet;
        }
    }

    LoadingCache<Long, Map<String, Object>> instanceData = CacheBuilder.newBuilder()
            .expireAfterAccess(24, TimeUnit.HOURS)
            .build(new CacheLoader<Long, Map<String, Object>>() {
                @Override
                public Map<String, Object> load(Long key) throws Exception {
                    return lookupCacheInstanceData(key);
                }
            });

    @Override
    public List<? extends Instance> getNonRemovedInstanceOn(Long hostId) {
        return create()
                .select(INSTANCE.fields())
                .from(INSTANCE)
                .join(INSTANCE_HOST_MAP)
                    .on(INSTANCE_HOST_MAP.HOST_ID.eq(hostId)
                            .and(INSTANCE_HOST_MAP.INSTANCE_ID.eq(INSTANCE.ID)))
                .where(INSTANCE.REMOVED.isNull().and(
                        INSTANCE.STATE.notIn(InstanceConstants.STATE_ERROR, InstanceConstants.STATE_ERRORING,
                                CommonStatesConstants.REMOVING)))
                .fetchInto(InstanceRecord.class);
    }

    @Override
    public Instance getInstanceByUuidOrExternalId(Long accountId, String uuid, String externalId) {
        Instance instance = null;
        Condition condition = INSTANCE.ACCOUNT_ID.eq(accountId).and(INSTANCE.STATE.notIn(CommonStatesConstants.PURGED,
                CommonStatesConstants.PURGING));

        if(StringUtils.isNotEmpty(uuid)) {
            instance = create()
                    .selectFrom(INSTANCE)
                    .where(condition
                    .and(INSTANCE.UUID.eq(uuid)))
                    .fetchAny();
        }

        if (instance == null && StringUtils.isNotEmpty(externalId)) {
            instance = create()
                    .selectFrom(INSTANCE)
                    .where(condition
                    .and(INSTANCE.EXTERNAL_ID.eq(externalId)))
                    .fetchAny();
        }

        return instance;
    }

    @Override
    public List<? extends Service> findServicesFor(Instance instance) {
        return create().select(SERVICE.fields())
                .from(SERVICE)
                .join(SERVICE_EXPOSE_MAP)
                .on(SERVICE_EXPOSE_MAP.SERVICE_ID.eq(SERVICE.ID))
                .where(SERVICE_EXPOSE_MAP.INSTANCE_ID.eq(instance.getId()))
                .fetchInto(ServiceRecord.class);
    }

    @Override
    public List<? extends Service> findServicesNonRemovedLinksOnly(Instance instance) {
        return create().select(SERVICE.fields())
                .from(SERVICE)
                .join(SERVICE_EXPOSE_MAP)
                .on(SERVICE_EXPOSE_MAP.SERVICE_ID.eq(SERVICE.ID))
                .where(SERVICE_EXPOSE_MAP.INSTANCE_ID.eq(instance.getId())
                        .and(SERVICE_EXPOSE_MAP.REMOVED.isNull()))
                .fetchInto(ServiceRecord.class);
    }

    @Override
    public List<? extends Instance> listNonRemovedInstances(Account account, boolean forService) {
        List<? extends Instance> serviceInstances = create().select(INSTANCE.fields())
                    .from(INSTANCE)
                    .join(SERVICE_EXPOSE_MAP)
                    .on(SERVICE_EXPOSE_MAP.INSTANCE_ID.eq(INSTANCE.ID))
                .where(INSTANCE.ACCOUNT_ID.eq(account.getId()))
                .and(INSTANCE.REMOVED.isNull())
                    .fetchInto(InstanceRecord.class);
        if (forService) {
            return serviceInstances;
        }
        List<? extends Instance> allInstances = create().select(INSTANCE.fields())
                .from(INSTANCE)
                .where(INSTANCE.ACCOUNT_ID.eq(account.getId()))
                .and(INSTANCE.REMOVED.isNull())
                .fetchInto(InstanceRecord.class);

        allInstances.removeAll(serviceInstances);
        return allInstances;
    }

    @Override
    public List<? extends Instance> findInstancesFor(Service service) {
        return create()
                .select(INSTANCE.fields())
                .from(INSTANCE)
                .join(SERVICE_EXPOSE_MAP)
                .on(SERVICE_EXPOSE_MAP.INSTANCE_ID.eq(INSTANCE.ID)
                        .and(SERVICE_EXPOSE_MAP.SERVICE_ID.eq(service.getId()))
                        .and(SERVICE_EXPOSE_MAP.STATE.in(CommonStatesConstants.ACTIVATING,
                                CommonStatesConstants.ACTIVE, CommonStatesConstants.REQUESTED))
                        .and(INSTANCE.STATE.notIn(CommonStatesConstants.PURGING, CommonStatesConstants.PURGED,
                                CommonStatesConstants.REMOVED, CommonStatesConstants.REMOVING)))
                .fetchInto(InstanceRecord.class);
    }

    @Override
    public List<? extends Instance> findInstanceByServiceName(long accountId, String serviceName) {
        return create().select(INSTANCE.fields())
            .from(INSTANCE)
            .join(SERVICE_EXPOSE_MAP)
                .on(INSTANCE.ID.eq(SERVICE_EXPOSE_MAP.INSTANCE_ID))
            .join(SERVICE)
                .on(SERVICE.ID.eq(SERVICE_EXPOSE_MAP.SERVICE_ID))
            .where(INSTANCE.STATE.eq(InstanceConstants.STATE_RUNNING)
                    .and(INSTANCE.ACCOUNT_ID.eq(accountId))
                    .and(SERVICE_EXPOSE_MAP.REMOVED.isNull())
                    .and(SERVICE.REMOVED.isNull())
                    .and(SERVICE.NAME.eq(serviceName)))
            .fetchInto(InstanceRecord.class);
    }

    @Override
    public List<? extends Instance> findInstanceByServiceName(long accountId, String serviceName, String stackName) {
        return create().select(INSTANCE.fields())
            .from(INSTANCE)
            .join(SERVICE_EXPOSE_MAP)
                .on(INSTANCE.ID.eq(SERVICE_EXPOSE_MAP.INSTANCE_ID))
            .join(SERVICE)
                .on(SERVICE.ID.eq(SERVICE_EXPOSE_MAP.SERVICE_ID))
            .join(STACK)
                .on(STACK.ID.eq(SERVICE.STACK_ID))
            .where(INSTANCE.STATE.eq(InstanceConstants.STATE_RUNNING)
                    .and(INSTANCE.ACCOUNT_ID.eq(accountId))
                    .and(SERVICE_EXPOSE_MAP.REMOVED.isNull())
                    .and(SERVICE.REMOVED.isNull())
                    .and(SERVICE.NAME.eq(serviceName))
                    .and(STACK.NAME.eq(stackName)))
            .fetchInto(InstanceRecord.class);
    }

    @Override
    public List<? extends Instance> findUnallocatedInstanceByDeploymentUnitUuid(long accountId, String deploymentUnitUuid) {
        return create().select(INSTANCE.fields())
                .from(INSTANCE)
                .leftOuterJoin(INSTANCE_HOST_MAP)
                    .on(INSTANCE_HOST_MAP.INSTANCE_ID.eq(INSTANCE.ID))
                .where(
                        INSTANCE.REMOVED.isNull()
                        .and(INSTANCE_HOST_MAP.ID.isNull())
                        .and(INSTANCE.DEPLOYMENT_UNIT_UUID.eq(deploymentUnitUuid))
                        .and(INSTANCE.ALLOCATION_STATE.eq(CommonStatesConstants.INACTIVE)))
                .fetchInto(InstanceRecord.class);
    }

    @Override
    public List<? extends Host> findHosts(long accountId, long instanceId) {
        return create().select(HOST.fields())
                .from(INSTANCE)
                .join(INSTANCE_HOST_MAP)
                    .on(INSTANCE.ID.eq(INSTANCE_HOST_MAP.INSTANCE_ID))
                .join(HOST)
                    .on(HOST.ID.eq(INSTANCE_HOST_MAP.HOST_ID))
                .where(HOST.REMOVED.isNull()
                    .and(INSTANCE_HOST_MAP.REMOVED.isNull())
                    .and(INSTANCE.ID.eq(instanceId)))
                .fetchInto(HostRecord.class);
    }

    protected Map<String, Object> lookupCacheInstanceData(long instanceId) {
        Instance instance = objectManager.loadResource(Instance.class, instanceId);
        if (instance == null) {
            return Collections.emptyMap();
        }
        Map<String, Object> newData = new HashMap<>();
        newData.put(DataUtils.FIELDS, instance.getData().get(DataUtils.FIELDS));
        return newData;
    }

    @Override
    public Map<String, Object> getCacheInstanceData(long instanceId) {
        return instanceData.getUnchecked(instanceId);
    }

    @Override
    public void clearCacheInstanceData(long instanceId) {
        instanceData.invalidate(instanceId);
    }

    @Override
    public List<PublicEndpoint> getPublicEndpoints(long accountId, Long serviceId, Long hostId) {
        List<PublicEndpoint> toReturn = new ArrayList<>();
        for (PublicEndpoint ep : getPublicEndpointsInternal(accountId, serviceId, hostId)) {
            if (ep.getHostId() != null && ep.getInstanceId() != null && !StringUtils.isEmpty(ep.getIpAddress())) {
                toReturn.add(ep);
            }
        }
        return toReturn;
    }

    private List<PublicEndpoint> getPublicEndpointsInternal(long accountId, Long serviceId, Long hostId) {
        MultiRecordMapper<PublicEndpoint> mapper = new MultiRecordMapper<PublicEndpoint>() {
            @Override
            protected PublicEndpoint map(List<Object> input) {
                Instance instance = (Instance) input.get(0);
                Port port = (Port) input.get(1);
                Host host = (Host) input.get(2);

                String address = "";
                IpAddress ip = (IpAddress) input.get(3);
                if (ip != null) {
                    address = ip.getAddress();
                } else {
                    address = DataAccessor.fieldString(port, PortConstants.FIELD_BIND_ADDR);
                }

                ServiceExposeMap exposeMap = (ServiceExposeMap) input.get(4);
                Long serviceId = exposeMap != null ? exposeMap.getServiceId() : null;
                PublicEndpoint data = new PublicEndpoint(address, port.getPublicPort(), host.getId(),
                        instance.getId(), serviceId);
                return data;
            }
        };

        InstanceTable instance = mapper.add(INSTANCE, INSTANCE.ID, INSTANCE.ACCOUNT_ID);
        PortTable port = mapper.add(PORT);
        HostTable host = mapper.add(HOST, HOST.ID);
        IpAddressTable ipAddress = mapper.add(IP_ADDRESS, IP_ADDRESS.ID, IP_ADDRESS.ADDRESS);
        ServiceExposeMapTable exposeMap = mapper.add(SERVICE_EXPOSE_MAP, SERVICE_EXPOSE_MAP.INSTANCE_ID,
                SERVICE_EXPOSE_MAP.SERVICE_ID);

        Condition condition = null;
        if (serviceId != null && hostId != null) {
            condition = host.ID.eq(hostId).and(exposeMap.SERVICE_ID.eq(serviceId));
        } else if (hostId != null) {
            condition = host.ID.eq(hostId);
        } else if (serviceId != null) {
            condition = (exposeMap.SERVICE_ID.eq(serviceId));
        }

        return create()
                .select(mapper.fields())
                .from(instance)
                .join(port)
                .on(port.INSTANCE_ID.eq(instance.ID))
                .join(INSTANCE_HOST_MAP)
                .on(INSTANCE_HOST_MAP.INSTANCE_ID.eq(instance.ID))
                .join(host)
                .on(INSTANCE_HOST_MAP.HOST_ID.eq(host.ID))
                .leftOuterJoin(ipAddress)
                .on(port.PUBLIC_IP_ADDRESS_ID.eq(ipAddress.ID))
                .leftOuterJoin(exposeMap)
                .on(exposeMap.INSTANCE_ID.eq(instance.ID))
                .where(instance.ACCOUNT_ID.eq(accountId))
                .and(instance.REMOVED.isNull())
                .and(port.REMOVED.isNull())
                .and(host.REMOVED.isNull())
                .and(ipAddress.REMOVED.isNull())
                .and(exposeMap.REMOVED.isNull())
                .and(port.PUBLIC_PORT.isNotNull())
                .and(port.STATE.in(CommonStatesConstants.ACTIVATING, CommonStatesConstants.ACTIVE,
                        CommonStatesConstants.UPDATING_ACTIVE))
                .and(condition)
                .fetch().map(mapper);
    }

    @Override
    public List<IpAddressToServiceIndex> getIpToIndex(Service service) {
        MultiRecordMapper<IpAddressToServiceIndex> mapper = new MultiRecordMapper<IpAddressToServiceIndex>() {
            @Override
            protected IpAddressToServiceIndex map(List<Object> input) {
                ServiceIndex index = (ServiceIndex) input.get(0);
                IpAddress ip = (IpAddress) input.get(1);
                Subnet subnet = (Subnet) input.get(2);
                IpAddressToServiceIndex data = new IpAddressToServiceIndex(index, ip, subnet);
                return data;
            }
        };

        ServiceIndexTable serviceIndex = mapper.add(SERVICE_INDEX);
        IpAddressTable ipAddress = mapper.add(IP_ADDRESS);
        SubnetTable subnet = mapper.add(SUBNET);
        ServiceExposeMapTable exposeMap = mapper.add(SERVICE_EXPOSE_MAP, SERVICE_EXPOSE_MAP.REMOVED);

        return create()
                .select(mapper.fields())
                .from(INSTANCE)
                .join(exposeMap)
                    .on(exposeMap.INSTANCE_ID.eq(INSTANCE.ID))
                .join(NIC)
                    .on(NIC.INSTANCE_ID.eq(exposeMap.INSTANCE_ID))
                .join(IP_ADDRESS_NIC_MAP)
                    .on(IP_ADDRESS_NIC_MAP.NIC_ID.eq(NIC.ID))
                .join(ipAddress)
                    .on(IP_ADDRESS_NIC_MAP.IP_ADDRESS_ID.eq(ipAddress.ID))
                .join(serviceIndex)
                    .on(serviceIndex.ID.eq(INSTANCE.SERVICE_INDEX_ID))
                .join(subnet)
                    .on(ipAddress.SUBNET_ID.eq(subnet.ID))
                .where(exposeMap.SERVICE_ID.eq(service.getId()))
                    .and(exposeMap.REMOVED.isNull())
                    .and(NIC.REMOVED.isNull())
                    .and(ipAddress.REMOVED.isNull())
                    .and(ipAddress.ADDRESS.isNotNull())
                    .and(INSTANCE.REMOVED.isNull())
                    .and(ipAddress.ROLE.eq(IpAddressConstants.ROLE_PRIMARY))
                .fetch().map(mapper);
    }
}
