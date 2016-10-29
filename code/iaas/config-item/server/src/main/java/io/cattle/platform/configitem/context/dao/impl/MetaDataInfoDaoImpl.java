package io.cattle.platform.configitem.context.dao.impl;

import static io.cattle.platform.core.model.tables.HealthcheckInstanceHostMapTable.*;
import static io.cattle.platform.core.model.tables.HostIpAddressMapTable.*;
import static io.cattle.platform.core.model.tables.HostTable.*;
import static io.cattle.platform.core.model.tables.InstanceHostMapTable.*;
import static io.cattle.platform.core.model.tables.InstanceTable.*;
import static io.cattle.platform.core.model.tables.IpAddressNicMapTable.*;
import static io.cattle.platform.core.model.tables.IpAddressTable.*;
import static io.cattle.platform.core.model.tables.NetworkTable.*;
import static io.cattle.platform.core.model.tables.NicTable.*;
import static io.cattle.platform.core.model.tables.ServiceExposeMapTable.*;

import io.cattle.platform.configitem.context.dao.MetaDataInfoDao;
import io.cattle.platform.configitem.context.data.metadata.common.ContainerMetaData;
import io.cattle.platform.configitem.context.data.metadata.common.HostMetaData;
import io.cattle.platform.configitem.context.data.metadata.common.NetworkMetaData;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.IpAddressConstants;
import io.cattle.platform.core.constants.NetworkConstants;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.dao.InstanceDao;
import io.cattle.platform.core.model.HealthcheckInstanceHostMap;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.IpAddress;
import io.cattle.platform.core.model.Network;
import io.cattle.platform.core.model.Nic;
import io.cattle.platform.core.model.ServiceExposeMap;
import io.cattle.platform.core.model.tables.HostTable;
import io.cattle.platform.core.model.tables.InstanceTable;
import io.cattle.platform.core.model.tables.IpAddressTable;
import io.cattle.platform.core.model.tables.NetworkTable;
import io.cattle.platform.core.model.tables.NicTable;
import io.cattle.platform.core.model.tables.ServiceExposeMapTable;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.db.jooq.mapper.MultiRecordMapper;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.DataAccessor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.jooq.JoinType;
import org.jooq.Record1;
import org.jooq.RecordHandler;

public class MetaDataInfoDaoImpl extends AbstractJooqDao implements MetaDataInfoDao {

    @Inject
    InstanceDao instanceDao;
    @Inject
    ObjectManager objMgr;

    private void populateContainerData(final Map<Long, IpAddress> instanceIdToHostIpMap,
            final Map<Long, HostMetaData> hostIdToHostMetadata, List<Object> input, ContainerMetaData data,
            Map<Long, String> instanceIdToUUID, Map<Long, List<HealthcheckInstanceHostMap>> instanceIdToHealthCheckers) {
        Instance instance = (Instance) input.get(0);
        instance.setData(instanceDao.getCacheInstanceData(instance.getId()));

        ServiceExposeMap serviceMap = input.get(1) != null ? (ServiceExposeMap) input.get(1) : null;
        String serviceIndex = DataAccessor.fieldString(instance,
                InstanceConstants.FIELD_SERVICE_INSTANCE_SERVICE_INDEX);
        Host host = null;
        if (input.get(2) != null) {
            host = (Host) input.get(2);
        }

        if (host != null) {
            HostMetaData hostMetaData = hostIdToHostMetadata.get(host.getId());
            List<String> healthCheckers = new ArrayList<>();
            if (instanceIdToHealthCheckers.get(instance.getId()) != null) {
                for (HealthcheckInstanceHostMap hostMap : instanceIdToHealthCheckers.get(instance.getId())) {
                    HostMetaData h = hostIdToHostMetadata.get(hostMap.getHostId());
                    if (h == null) {
                        continue;
                    }
                    healthCheckers.add(h.getUuid());
                }
            }
            data.setInstanceAndHostMetadata(instance, hostMetaData, healthCheckers);
        }

        data.setExposeMap(serviceMap);
        data.setService_index(serviceIndex);

        String primaryIp = null;

        if (input.size() > 3) {
            if (input.get(3) != null) {
                primaryIp = ((IpAddress) input.get(3)).getAddress();
            }

            if (instanceIdToHostIpMap != null && instanceIdToHostIpMap.containsKey(instance.getId())) {
                data.setIp(instanceIdToHostIpMap.get(instance.getId()).getAddress());
            } else {
                data.setIp(primaryIp);
            }
            Nic nic = null;
            if (input.get(4) != null) {
                nic = (Nic) input.get(4);
                data.setNicInformation(nic);
            }

            Network ntwk = null;
            if (input.get(5) != null) {
                ntwk = (Network) input.get(5);
                data.setNetwork_uuid(ntwk.getUuid());
            }
        }
        if (instance.getNetworkContainerId() != null) {
            String parentInstanceUUID = instanceIdToUUID.get(instance.getNetworkContainerId());
            if (parentInstanceUUID != null) {
                data.setNetwork_from_container_uuid(parentInstanceUUID);
            }
        }
    }

    @Override
    public List<ContainerMetaData> getNetworkFromContainersData(long accountId,
            final Map<Long, String> instanceIdToUUID,
            final Map<Long, List<HealthcheckInstanceHostMap>> instanceIdToHealthCheckers,
            final Map<Long, IpAddress> instanceIdToHostIpMap,
            final Map<Long, HostMetaData> hostIdToHostMetadata) {

        MultiRecordMapper<ContainerMetaData> mapper = new MultiRecordMapper<ContainerMetaData>() {
            @Override
            protected ContainerMetaData map(List<Object> input) {
                ContainerMetaData data = new ContainerMetaData();

                populateContainerData(instanceIdToHostIpMap, hostIdToHostMetadata, input, data,
                        instanceIdToUUID, instanceIdToHealthCheckers);

                return data;
            }
        };

        InstanceTable instance = mapper.add(INSTANCE, INSTANCE.UUID, INSTANCE.NAME, INSTANCE.CREATE_INDEX,
                INSTANCE.HEALTH_STATE,
                INSTANCE.START_COUNT, INSTANCE.STATE, INSTANCE.EXTERNAL_ID, INSTANCE.MEMORY_RESERVATION,
                INSTANCE.MILLI_CPU_RESERVATION,
                INSTANCE.NETWORK_CONTAINER_ID,
                INSTANCE.SYSTEM);
        ServiceExposeMapTable exposeMap = mapper.add(SERVICE_EXPOSE_MAP, SERVICE_EXPOSE_MAP.SERVICE_ID,
                SERVICE_EXPOSE_MAP.DNS_PREFIX, SERVICE_EXPOSE_MAP.UPGRADE);
        HostTable host = mapper.add(HOST, HOST.ID);
        return create()
                .select(mapper.fields())
                .from(instance)
                .join(INSTANCE_HOST_MAP)
                .on(instance.ID.eq(INSTANCE_HOST_MAP.INSTANCE_ID))
                .join(host)
                .on(host.ID.eq(INSTANCE_HOST_MAP.HOST_ID))
                .join(exposeMap, JoinType.LEFT_OUTER_JOIN)
                .on(exposeMap.INSTANCE_ID.eq(instance.ID))
                .where(instance.ACCOUNT_ID.eq(accountId))
                .and(instance.REMOVED.isNull())
                .and(instance.STATE.notIn(CommonStatesConstants.REMOVING, CommonStatesConstants.REMOVED))
                .and(instance.NETWORK_CONTAINER_ID.isNotNull())
                .and(exposeMap.REMOVED.isNull())
                .and((host.REMOVED.isNull()))
                .and(exposeMap.STATE.isNull().or(
                        exposeMap.STATE.notIn(CommonStatesConstants.REMOVING, CommonStatesConstants.REMOVED)))
                .and(exposeMap.UPGRADE.isNull().or(exposeMap.UPGRADE.eq(false)))
                .fetch().map(mapper);
    }

    @Override
    public List<ContainerMetaData> getManagedContainersData(long accountId,
            final Map<Long, List<HealthcheckInstanceHostMap>> instanceIdToHealthCheckers,
            final Map<Long, IpAddress> instanceIdToHostIpMap, final Map<Long, HostMetaData> hostIdToHostMetadata) {

        MultiRecordMapper<ContainerMetaData> mapper = new MultiRecordMapper<ContainerMetaData>() {
            @Override
            protected ContainerMetaData map(List<Object> input) {
                ContainerMetaData data = new ContainerMetaData();
                populateContainerData(instanceIdToHostIpMap, hostIdToHostMetadata, input, data,
                        new HashMap<Long, String>(), instanceIdToHealthCheckers);

                return data;
            }
        };

        InstanceTable instance = mapper.add(INSTANCE, INSTANCE.UUID, INSTANCE.NAME, INSTANCE.CREATE_INDEX,
                INSTANCE.HEALTH_STATE, INSTANCE.START_COUNT, INSTANCE.STATE, INSTANCE.EXTERNAL_ID,
                INSTANCE.DNS_INTERNAL, INSTANCE.DNS_SEARCH_INTERNAL, INSTANCE.MEMORY_RESERVATION, INSTANCE.MILLI_CPU_RESERVATION);
        ServiceExposeMapTable exposeMap = mapper.add(SERVICE_EXPOSE_MAP, SERVICE_EXPOSE_MAP.SERVICE_ID,
                SERVICE_EXPOSE_MAP.DNS_PREFIX, SERVICE_EXPOSE_MAP.UPGRADE);
        HostTable host = mapper.add(HOST, HOST.ID);
        IpAddressTable instanceIpAddress = mapper.add(IP_ADDRESS, IP_ADDRESS.ADDRESS);
        NicTable nic = mapper.add(NIC, NIC.ID, NIC.INSTANCE_ID, NIC.MAC_ADDRESS);
        NetworkTable ntwk = mapper.add(NETWORK, NETWORK.UUID);
        return create()
                .select(mapper.fields())
                .from(instance)
                .join(INSTANCE_HOST_MAP)
                .on(instance.ID.eq(INSTANCE_HOST_MAP.INSTANCE_ID))
                .join(host)
                .on(host.ID.eq(INSTANCE_HOST_MAP.HOST_ID))
                .join(exposeMap, JoinType.LEFT_OUTER_JOIN)
                .on(exposeMap.INSTANCE_ID.eq(instance.ID))
                .join(nic)
                .on(nic.INSTANCE_ID.eq(INSTANCE_HOST_MAP.INSTANCE_ID))
                .join(IP_ADDRESS_NIC_MAP)
                .on(IP_ADDRESS_NIC_MAP.NIC_ID.eq(nic.ID))
                .join(instanceIpAddress)
                .on(instanceIpAddress.ID.eq(IP_ADDRESS_NIC_MAP.IP_ADDRESS_ID))
                .join(ntwk)
                .on(nic.NETWORK_ID.eq(ntwk.ID))
                .where(instance.ACCOUNT_ID.eq(accountId))
                .and(instance.REMOVED.isNull())
                .and(instance.STATE.notIn(CommonStatesConstants.REMOVING, CommonStatesConstants.REMOVED))
                .and(exposeMap.REMOVED.isNull())
                .and(instanceIpAddress.ROLE.eq(IpAddressConstants.ROLE_PRIMARY))
                .and(ntwk.REMOVED.isNull())
                .and((host.REMOVED.isNull()))
                .and(exposeMap.STATE.isNull().or(
                        exposeMap.STATE.notIn(CommonStatesConstants.REMOVING, CommonStatesConstants.REMOVED)))
                .and(exposeMap.UPGRADE.isNull().or(exposeMap.UPGRADE.eq(false)))
                .fetch().map(mapper);
    }

    @Override
    public List<String> getPrimaryIpsOnInstanceHost(final long hostId) {
        final List<String> ips = new ArrayList<>();
        create().select(IP_ADDRESS.ADDRESS)
                .from(IP_ADDRESS)
                .join(IP_ADDRESS_NIC_MAP)
                        .on(IP_ADDRESS.ID.eq(IP_ADDRESS_NIC_MAP.IP_ADDRESS_ID))
                        .join(NIC)
                        .on(NIC.ID.eq(IP_ADDRESS_NIC_MAP.NIC_ID))
                        .join(INSTANCE_HOST_MAP)
                        .on(INSTANCE_HOST_MAP.INSTANCE_ID.eq(NIC.INSTANCE_ID))
                        .where(INSTANCE_HOST_MAP.HOST_ID.eq(hostId)
                                .and(NIC.REMOVED.isNull())
                                .and(IP_ADDRESS_NIC_MAP.REMOVED.isNull())
                                .and(IP_ADDRESS.REMOVED.isNull())
                                .and(IP_ADDRESS.ROLE.eq(IpAddressConstants.ROLE_PRIMARY))
                        )
                .fetchInto(new RecordHandler<Record1<String>>() {
                    @Override
                    public void next(Record1<String> record) {
                        if (StringUtils.isNotBlank(record.value1())) {
                            ips.add(record.value1());
                        }
                    }
                });
        return ips;
    }

    @Override
    public Map<Long, HostMetaData> getHostIdToHostMetadata(long accountId) {
        Map<Long, HostMetaData> toReturn = new HashMap<>();
        List<HostMetaData> hosts = getAllInstanceHostMetaData(accountId);
        for (HostMetaData host : hosts) {
            toReturn.put(host.getHostId(), host);
        }
        return toReturn;
    }

    @Override
    public Map<Long, List<HealthcheckInstanceHostMap>> getInstanceIdToHealthCheckers(long accountId) {
        Map<Long, List<HealthcheckInstanceHostMap>> instanceIdToHealthCheckers = new HashMap<>();
        List<? extends HealthcheckInstanceHostMap> hostMaps = objMgr.find(HealthcheckInstanceHostMap.class,
                HEALTHCHECK_INSTANCE_HOST_MAP.ACCOUNT_ID, accountId, HEALTHCHECK_INSTANCE_HOST_MAP.REMOVED, null);
        for (HealthcheckInstanceHostMap hostMap : hostMaps) {
            List<HealthcheckInstanceHostMap> instanceHostMaps = instanceIdToHealthCheckers
                    .get(hostMap.getInstanceId());
            if (instanceHostMaps == null) {
                instanceHostMaps = new ArrayList<>();
            }
            instanceHostMaps.add(hostMap);
            instanceIdToHealthCheckers.put(hostMap.getInstanceId(), instanceHostMaps);
        }
        return instanceIdToHealthCheckers;
    }

    protected List<HostMetaData> getAllInstanceHostMetaData(long accountId) {
        MultiRecordMapper<HostMetaData> mapper = new MultiRecordMapper<HostMetaData>() {
            @Override
            protected HostMetaData map(List<Object> input) {
                Host host = (Host)input.get(0);
                IpAddress hostIp = (IpAddress)input.get(1);
                HostMetaData data = new HostMetaData(hostIp.getAddress(), host);
                return data;
            }
        };

        HostTable host = mapper.add(HOST);
        IpAddressTable hostIpAddress = mapper.add(IP_ADDRESS);

        return create()
                .select(mapper.fields())
                .from(hostIpAddress)
                .join(HOST_IP_ADDRESS_MAP)
                .on(HOST_IP_ADDRESS_MAP.IP_ADDRESS_ID.eq(hostIpAddress.ID))
                .join(host)
                .on(host.ID.eq(HOST_IP_ADDRESS_MAP.HOST_ID))
                .where(host.REMOVED.isNull())
                .and(host.STATE.notIn(CommonStatesConstants.REMOVING, CommonStatesConstants.REMOVED))
                .and(hostIpAddress.REMOVED.isNull())
                .and(host.ACCOUNT_ID.eq(accountId))
                .fetch().map(mapper);
    }

    @Override
    public List<HostMetaData> getInstanceHostMetaData(long accountId, long instanceId) {
        MultiRecordMapper<HostMetaData> mapper = new MultiRecordMapper<HostMetaData>() {
            @Override
            protected HostMetaData map(List<Object> input) {
                Host host = (Host) input.get(0);
                IpAddress hostIp = (IpAddress) input.get(1);
                HostMetaData data = new HostMetaData(hostIp.getAddress(), host);
                return data;
            }
        };

        HostTable host = mapper.add(HOST);
        IpAddressTable hostIpAddress = mapper.add(IP_ADDRESS);

        return create()
                .select(mapper.fields())
                .from(hostIpAddress)
                .join(HOST_IP_ADDRESS_MAP)
                .on(HOST_IP_ADDRESS_MAP.IP_ADDRESS_ID.eq(hostIpAddress.ID))
                .join(host)
                .on(host.ID.eq(HOST_IP_ADDRESS_MAP.HOST_ID))
                .join(INSTANCE_HOST_MAP)
                .on(host.ID.eq(INSTANCE_HOST_MAP.HOST_ID))
                .where(host.REMOVED.isNull())
                .and(host.STATE.notIn(CommonStatesConstants.REMOVING, CommonStatesConstants.REMOVED))
                .and(INSTANCE_HOST_MAP.INSTANCE_ID.eq(instanceId))
                .and(hostIpAddress.REMOVED.isNull())
                .and(host.ACCOUNT_ID.eq(accountId))
                .fetch().map(mapper);
    }

    @Override
    public List<NetworkMetaData> getNetworksMetaData(long accountId) {
        MultiRecordMapper<NetworkMetaData> mapper = new MultiRecordMapper<NetworkMetaData>() {
            @Override
            protected NetworkMetaData map(List<Object> input) {
                Network ntwk = (Network) input.get(0);
                Map<String, Object> meta = DataAccessor.fieldMap(ntwk, ServiceConstants.FIELD_METADATA);
                NetworkMetaData data = new NetworkMetaData(ntwk.getName(), ntwk.getUuid(), DataAccessor.fieldBool(ntwk, NetworkConstants.FIELD_HOST_PORTS), meta);
                return data;
            }
        };

        NetworkTable ntwk = mapper.add(NETWORK, NETWORK.NAME, NETWORK.UUID, NETWORK.DATA);
        return create()
                .select(mapper.fields())
                .from(ntwk)
                .where(ntwk.REMOVED.isNull())
                .and(ntwk.ACCOUNT_ID.eq(accountId))
                .fetch().map(mapper);
    }
}
