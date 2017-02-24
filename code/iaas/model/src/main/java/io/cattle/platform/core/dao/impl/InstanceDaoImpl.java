package io.cattle.platform.core.dao.impl;

import static io.cattle.platform.core.model.tables.HostTable.*;
import static io.cattle.platform.core.model.tables.HostIpAddressMapTable.*;
import static io.cattle.platform.core.model.tables.InstanceHostMapTable.*;
import static io.cattle.platform.core.model.tables.InstanceLinkTable.*;
import static io.cattle.platform.core.model.tables.InstanceRevisionTable.*;
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
import io.cattle.platform.core.dao.InstanceDao;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.InstanceHostMap;
import io.cattle.platform.core.model.InstanceLink;
import io.cattle.platform.core.model.InstanceRevision;
import io.cattle.platform.core.model.IpAddress;
import io.cattle.platform.core.model.Nic;
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
import io.cattle.platform.core.model.tables.records.InstanceHostMapRecord;
import io.cattle.platform.core.model.tables.records.InstanceLinkRecord;
import io.cattle.platform.core.model.tables.records.InstanceRecord;
import io.cattle.platform.core.model.tables.records.NicRecord;
import io.cattle.platform.core.model.tables.records.ServiceRecord;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.db.jooq.mapper.MultiRecordMapper;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.object.util.DataUtils;
import io.cattle.platform.util.type.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.jooq.Condition;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

@Named
public class InstanceDaoImpl extends AbstractJooqDao implements InstanceDao {
    @Inject
    ObjectManager objectManager;

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
    public List<? extends Instance> listNonRemovedNonStackInstances(Account account) {
        return create().select(INSTANCE.fields())
                .from(INSTANCE)
                .where(INSTANCE.ACCOUNT_ID.eq(account.getId()))
                .and(INSTANCE.REMOVED.isNull())
                .and(INSTANCE.STACK_ID.isNull())
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
                address = DataAccessor.fieldString(port, PortConstants.FIELD_BIND_ADDR);
                if (StringUtils.isEmpty(address) || "0.0.0.0".equals(address)) {
                    IpAddress ip = (IpAddress) input.get(3);
                    if (ip != null) {
                        address = ip.getAddress();
                    } else {
                        IpAddress hostIp = (IpAddress) input.get(4);
                        if (hostIp != null) {
                            address = hostIp.getAddress();
                        }
                    }
                }

                ServiceExposeMap exposeMap = (ServiceExposeMap) input.get(5);
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
        IpAddressTable hostIp = mapper.add(IP_ADDRESS, IP_ADDRESS.ID, IP_ADDRESS.ADDRESS);
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
                .leftOuterJoin(HOST_IP_ADDRESS_MAP)
                .on(host.ID.eq(HOST_IP_ADDRESS_MAP.HOST_ID))
                .leftOuterJoin(hostIp)
                .on(HOST_IP_ADDRESS_MAP.IP_ADDRESS_ID.eq(hostIp.ID))
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

    @Override
    public List<? extends Instance> findBadInstances(int count) {
        return create().select(INSTANCE.fields())
            .from(INSTANCE)
            .join(INSTANCE_HOST_MAP)
                .on(INSTANCE_HOST_MAP.INSTANCE_ID.eq(INSTANCE.ID))
            .join(HOST)
                .on(INSTANCE_HOST_MAP.HOST_ID.eq(HOST.ID))
            .where(HOST.REMOVED.isNotNull().and(INSTANCE.REMOVED.isNull())
                    .and(INSTANCE.STATE.notIn(InstanceConstants.STATE_STOPPING, CommonStatesConstants.REMOVING)))
            .limit(count)
            .fetchInto(InstanceRecord.class);
    }

    @Override
    public List<? extends InstanceHostMap> findBadInstanceHostMaps(int count) {
        return create().select(INSTANCE_HOST_MAP.fields())
            .from(INSTANCE_HOST_MAP)
            .join(INSTANCE)
                .on(INSTANCE_HOST_MAP.INSTANCE_ID.eq(INSTANCE.ID))
            .where(INSTANCE_HOST_MAP.REMOVED.isNull()
                    .and(INSTANCE.STATE.eq(CommonStatesConstants.PURGED))
                    .and(INSTANCE_HOST_MAP.STATE.notIn(CommonStatesConstants.DEACTIVATING, CommonStatesConstants.REMOVING)))
            .limit(count)
            .fetchInto(InstanceHostMapRecord.class);
    }

    @Override
    public List<? extends Nic> findBadNics(int count) {
        return create().select(NIC.fields())
                .from(NIC)
                .join(INSTANCE)
                    .on(INSTANCE.ID.eq(NIC.INSTANCE_ID))
                .where(NIC.REMOVED.isNull().and(INSTANCE.STATE.eq(CommonStatesConstants.PURGED))
                        .and(NIC.STATE.notIn(CommonStatesConstants.DEACTIVATING, CommonStatesConstants.REMOVING)))
                .limit(count)
                .fetchInto(NicRecord.class);
    }

    @Override
    public List<? extends InstanceLink> findBadInstanceLinks(int count) {
        return create().select(INSTANCE_LINK.fields())
                .from(INSTANCE_LINK)
                .join(INSTANCE)
                    .on(INSTANCE.ID.eq(INSTANCE_LINK.TARGET_INSTANCE_ID))
                .where(INSTANCE.STATE.eq(CommonStatesConstants.PURGED))
                .limit(count)
                .fetchInto(InstanceLinkRecord.class);
    }

    @Override
    public InstanceRevision createRevision(Instance instance, Map<String, Object> spec) {
        InstanceRevision revision = objectManager.findAny(InstanceRevision.class, INSTANCE_REVISION.INSTANCE_ID,
                instance.getId(),
                INSTANCE_REVISION.REMOVED, null);
        if (revision == null) {
            Map<String, Object> data = new HashMap<>();
            Map<String, Map<String, Object>> specs = new HashMap<>();
            String name = instance.getUuid();
            specs.put(name, spec);
            data.put(InstanceConstants.FIELD_INSTANCE_SPECS, specs);
            data.put(ObjectMetaDataManager.NAME_FIELD, name);
            data.put(ObjectMetaDataManager.ACCOUNT_FIELD, instance.getAccountId());
            data.put("instanceId", instance.getId());
            revision = objectManager.create(InstanceRevision.class, data);
        }
        return revision;
    }

    @Override
    public void cleanupInstanceRevisions(Instance instance) {
        List<InstanceRevision> revisions = objectManager.find(InstanceRevision.class, INSTANCE_REVISION.INSTANCE_ID,
                instance.getId(),
                INSTANCE_REVISION.REMOVED, null);
        for (InstanceRevision revision : revisions) {
            Map<String, Object> params = new HashMap<>();
            params.put(ObjectMetaDataManager.REMOVED_FIELD, new Date());
            params.put(ObjectMetaDataManager.REMOVE_TIME_FIELD, new Date());
            params.put(ObjectMetaDataManager.STATE_FIELD, CommonStatesConstants.REMOVED);
            objectManager.setFields(revision, params);
        }
    }

    @Override
    public Map<String, Object> getInstanceSpec(Instance instance) {
        InstanceRevision revision = objectManager.findAny(InstanceRevision.class, INSTANCE_REVISION.ID,
                instance.getRevisionId());
        if (revision == null) {
            return null;
        }
        Map<?, ?> specs = CollectionUtils.toMap(DataAccessor.field(
                revision, InstanceConstants.FIELD_INSTANCE_SPECS, Object.class));
        if (specs.size() != 0) {
            return CollectionUtils.toMap(specs.get(instance.getUuid()));
        }
        return null;
    }
}
