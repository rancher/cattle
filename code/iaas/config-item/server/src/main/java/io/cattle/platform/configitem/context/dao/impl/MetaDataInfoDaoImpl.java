package io.cattle.platform.configitem.context.dao.impl;

import static io.cattle.platform.core.model.tables.HealthcheckInstanceHostMapTable.*;
import static io.cattle.platform.core.model.tables.HostIpAddressMapTable.*;
import static io.cattle.platform.core.model.tables.HostTable.*;
import static io.cattle.platform.core.model.tables.InstanceHostMapTable.*;
import static io.cattle.platform.core.model.tables.InstanceLinkTable.*;
import static io.cattle.platform.core.model.tables.InstanceTable.*;
import static io.cattle.platform.core.model.tables.IpAddressNicMapTable.*;
import static io.cattle.platform.core.model.tables.IpAddressTable.*;
import static io.cattle.platform.core.model.tables.NetworkTable.*;
import static io.cattle.platform.core.model.tables.NicTable.*;
import static io.cattle.platform.core.model.tables.ServiceConsumeMapTable.*;
import static io.cattle.platform.core.model.tables.ServiceExposeMapTable.*;
import static io.cattle.platform.core.model.tables.ServiceTable.*;
import static io.cattle.platform.core.model.tables.StackTable.*;
import io.cattle.platform.configitem.context.dao.MetaDataInfoDao;
import io.cattle.platform.configitem.context.data.metadata.common.ContainerLinkMetaData;
import io.cattle.platform.configitem.context.data.metadata.common.ContainerMetaData;
import io.cattle.platform.configitem.context.data.metadata.common.DefaultMetaData;
import io.cattle.platform.configitem.context.data.metadata.common.HostMetaData;
import io.cattle.platform.configitem.context.data.metadata.common.MetaHelperInfo;
import io.cattle.platform.configitem.context.data.metadata.common.NetworkMetaData;
import io.cattle.platform.configitem.context.data.metadata.common.ServiceContainerLinkMetaData;
import io.cattle.platform.configitem.context.data.metadata.common.ServiceLinkMetaData;
import io.cattle.platform.configitem.context.data.metadata.common.ServiceMetaData;
import io.cattle.platform.configitem.context.data.metadata.common.StackMetaData;
import io.cattle.platform.core.addon.InstanceHealthCheck;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.IpAddressConstants;
import io.cattle.platform.core.constants.NetworkConstants;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.dao.InstanceDao;
import io.cattle.platform.core.dao.LoadBalancerInfoDao;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.HealthcheckInstanceHostMap;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.IpAddress;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.tables.HostTable;
import io.cattle.platform.core.model.tables.InstanceHostMapTable;
import io.cattle.platform.core.model.tables.InstanceTable;
import io.cattle.platform.core.model.tables.IpAddressTable;
import io.cattle.platform.core.model.tables.ServiceExposeMapTable;
import io.cattle.platform.core.model.tables.ServiceTable;
import io.cattle.platform.core.model.tables.records.InstanceRecord;
import io.cattle.platform.core.model.tables.records.NetworkRecord;
import io.cattle.platform.core.model.tables.records.ServiceRecord;
import io.cattle.platform.core.util.ServiceUtil;
import io.cattle.platform.core.util.LBMetadataUtil.LBConfigMetadataStyle;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.db.jooq.mapper.MultiRecordMapper;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.util.exception.ExceptionUtils;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.jooq.Condition;
import org.jooq.JoinType;
import org.jooq.Record11;
import org.jooq.Record20;
import org.jooq.Record3;
import org.jooq.Record4;
import org.jooq.Record5;
import org.jooq.RecordHandler;

public class MetaDataInfoDaoImpl extends AbstractJooqDao implements MetaDataInfoDao {

    @Inject
    InstanceDao instanceDao;
    @Inject
    ObjectManager objMgr;
    @Inject
    LoadBalancerInfoDao lbInfoDao;
    @Inject
    JsonMapper jsonMapper;

    @Override
    public void fetchContainers(final MetaHelperInfo helperInfo,
            final OutputStream os) {

        Condition condition = getMultiAccountInstanceSearchCondition(helperInfo);
        final InstanceTable targetInstance = INSTANCE.as("target_instance");
        create()
                .select(INSTANCE.ID, INSTANCE.ACCOUNT_ID, INSTANCE.UUID, INSTANCE.NAME, INSTANCE.CREATE_INDEX,
                        INSTANCE.HEALTH_STATE, INSTANCE.START_COUNT, INSTANCE.STATE, INSTANCE.EXTERNAL_ID,
                        INSTANCE.DNS_INTERNAL, INSTANCE.DNS_SEARCH_INTERNAL, INSTANCE.MEMORY_RESERVATION,
                        INSTANCE.MILLI_CPU_RESERVATION, INSTANCE.SYSTEM, IP_ADDRESS.ADDRESS, NIC.MAC_ADDRESS,
                        NETWORK.UUID, NETWORK.KIND, HOST.ID, targetInstance.UUID)
                .from(INSTANCE)
                .leftOuterJoin(targetInstance)
                .on(INSTANCE.NETWORK_CONTAINER_ID.eq(targetInstance.ID))
                .join(INSTANCE_HOST_MAP)
                .on(INSTANCE.ID.eq(INSTANCE_HOST_MAP.INSTANCE_ID))
                .join(HOST)
                .on(HOST.ID.eq(INSTANCE_HOST_MAP.HOST_ID))
                .join(SERVICE_EXPOSE_MAP, JoinType.LEFT_OUTER_JOIN)
                .on(SERVICE_EXPOSE_MAP.INSTANCE_ID.eq(INSTANCE.ID))
                .join(NIC)
                .on(NIC.INSTANCE_ID.eq(INSTANCE_HOST_MAP.INSTANCE_ID))
                .leftOuterJoin(IP_ADDRESS_NIC_MAP)
                .on(IP_ADDRESS_NIC_MAP.NIC_ID.eq(NIC.ID))
                .leftOuterJoin(IP_ADDRESS)
                .on(IP_ADDRESS.ID.eq(IP_ADDRESS_NIC_MAP.IP_ADDRESS_ID))
                .join(NETWORK)
                .on(NIC.NETWORK_ID.eq(NETWORK.ID))
                .where(INSTANCE.REMOVED.isNull())
                .and(INSTANCE.STATE.notIn(CommonStatesConstants.REMOVING, CommonStatesConstants.REMOVED,
                        InstanceConstants.STATE_ERROR, InstanceConstants.STATE_ERRORING))
                .and(SERVICE_EXPOSE_MAP.REMOVED.isNull())
                .and(IP_ADDRESS.ROLE.eq(IpAddressConstants.ROLE_PRIMARY).or(
                        IP_ADDRESS.ROLE.isNull().and(NETWORK.KIND.eq(NetworkConstants.KIND_DOCKER_HOST)))
                        .or(IP_ADDRESS.ROLE.isNull().and(INSTANCE.NETWORK_CONTAINER_ID.isNotNull())))
                .and(NETWORK.REMOVED.isNull())
                .and((HOST.REMOVED.isNull()))
                .and(targetInstance.REMOVED.isNull())
                .and(SERVICE_EXPOSE_MAP.STATE.isNull().or(
                        SERVICE_EXPOSE_MAP.STATE.notIn(CommonStatesConstants.REMOVING, CommonStatesConstants.REMOVED)))
                .and(SERVICE_EXPOSE_MAP.UPGRADE.isNull().or(SERVICE_EXPOSE_MAP.UPGRADE.eq(false)))
                .and(condition)
                .fetchInto(
                        new RecordHandler<Record20<Long, Long, String, String, Long, String, Long, String, String, String, String, Long, Long, Boolean, String, String, String, String, Long, String>>() {
                            @Override
                            public void next(
                                    Record20<Long, Long, String, String, Long, String, Long, String, String, String, String, Long, Long, Boolean, String, String, String, String, Long, String> record) {
                                InstanceRecord instance = new InstanceRecord();
                                instance.setId(record.getValue(INSTANCE.ID));
                                instance.setName(record.getValue(INSTANCE.NAME));
                                instance.setUuid(record.getValue(INSTANCE.UUID));
                                instance.setCreateIndex(record.getValue(INSTANCE.CREATE_INDEX));
                                instance.setHealthState(record.getValue(INSTANCE.HEALTH_STATE));
                                instance.setStartCount(record.getValue(INSTANCE.START_COUNT));
                                instance.setState(record.getValue(INSTANCE.STATE));
                                instance.setExternalId(record.getValue(INSTANCE.EXTERNAL_ID));
                                instance.setDnsInternal(record.getValue(INSTANCE.DNS_INTERNAL));
                                instance.setDnsSearchInternal(record.getValue(INSTANCE.DNS_SEARCH_INTERNAL));
                                instance.setMemoryReservation(record.getValue(INSTANCE.MEMORY_RESERVATION));
                                instance.setMilliCpuReservation(record.getValue(INSTANCE.MILLI_CPU_RESERVATION));
                                instance.setSystem(record.getValue(INSTANCE.SYSTEM));
                                instance.setAccountId(record.getValue(INSTANCE.ACCOUNT_ID));
                                String primaryIp = record.getValue(IP_ADDRESS.ADDRESS);
                                String macAddress = record.getValue(NIC.MAC_ADDRESS);
                                String networkUUID = record.getValue(NETWORK.UUID);
                                String networkKind = record.getValue(NETWORK.KIND);
                                Long hostId = record.getValue(HOST.ID);
                                String targetInstanceUUID = record.getValue(targetInstance.UUID);

                                ContainerMetaData data = new ContainerMetaData();
                                instance.setData(instanceDao.getCacheInstanceData(instance.getId()));
                                String serviceIndex = DataAccessor.fieldString(instance,
                                        InstanceConstants.FIELD_SERVICE_INSTANCE_SERVICE_INDEX);

                                HostMetaData hostMetaData = helperInfo.getHostIdToHostMetadata().get(hostId);
                                List<String> healthCheckers = new ArrayList<>();
                                if (helperInfo.getInstanceIdToHealthCheckers().get(instance.getId()) != null) {
                                    for (HealthcheckInstanceHostMap hostMap : helperInfo.getInstanceIdToHealthCheckers().get(
                                            instance.getId())) {
                                        HostMetaData h = helperInfo.getHostIdToHostMetadata().get(hostMap.getHostId());
                                        if (h == null) {
                                            continue;
                                        }
                                        healthCheckers.add(h.getUuid());
                                    }
                                }
                                data.setInstanceAndHostMetadata(instance, hostMetaData, healthCheckers,
                                        helperInfo.getAccounts().get(instance.getAccountId()));

                                data.setService_index(serviceIndex);
                                if (networkKind.equalsIgnoreCase(NetworkConstants.KIND_DOCKER_HOST)
                                        && hostMetaData != null) {
                                    primaryIp = hostMetaData.getAgent_ip();
                                }
                                data.setIp(primaryIp);
                                data.setNetwork_uuid(networkUUID);
                                data.setNetwork_from_container_uuid(targetInstanceUUID);
                                data.setPrimary_mac_address(macAddress);

                                writeToJson(os, data);
                            }
                        });
    }

    @Override
    public Map<Long, HostMetaData> getHostIdToHostMetadata(Account account, Map<Long, Account> accounts,
            Set<Long> linkedServicesIds) {
        Map<Long, HostMetaData> toReturn = new HashMap<>();
        List<HostMetaData> hosts = getAllInstanceHostMetaDataForAccount(account);
        if (!linkedServicesIds.isEmpty()) {
            hosts.addAll(getAllInstanceHostMetaDataForLinkedServices(accounts, linkedServicesIds));
        }

        for (HostMetaData host : hosts) {
            toReturn.put(host.getHostId(), host);
        }
        return toReturn;
    }

    protected List<HostMetaData> getAllInstanceHostMetaDataForLinkedServices(final Map<Long, Account> accounts,
            final Set<Long> linkedServicesIds) {
        MultiRecordMapper<HostMetaData> mapper = new MultiRecordMapper<HostMetaData>() {
            @Override
            protected HostMetaData map(List<Object> input) {
                Host host = (Host) input.get(0);
                IpAddress hostIp = (IpAddress) input.get(1);
                HostMetaData data = new HostMetaData(hostIp.getAddress(), host, accounts.get(host.getAccountId()));
                return data;
            }
        };

        HostTable host = mapper.add(HOST);
        IpAddressTable hostIpAddress = mapper.add(IP_ADDRESS);
        InstanceHostMapTable instanceHostMap = mapper.add(INSTANCE_HOST_MAP, INSTANCE_HOST_MAP.INSTANCE_ID,
                INSTANCE_HOST_MAP.HOST_ID);
        ServiceExposeMapTable exposeMap = mapper.add(SERVICE_EXPOSE_MAP, SERVICE_EXPOSE_MAP.INSTANCE_ID,
                SERVICE_EXPOSE_MAP.SERVICE_ID);
        return create()
                .select(mapper.fields())
                .from(hostIpAddress)
                .join(HOST_IP_ADDRESS_MAP)
                .on(HOST_IP_ADDRESS_MAP.IP_ADDRESS_ID.eq(hostIpAddress.ID))
                .join(host)
                .on(host.ID.eq(HOST_IP_ADDRESS_MAP.HOST_ID))
                .join(instanceHostMap)
                .on(instanceHostMap.HOST_ID.eq(host.ID))
                .join(exposeMap)
                .on(exposeMap.INSTANCE_ID.eq(instanceHostMap.INSTANCE_ID))
                .where(host.REMOVED.isNull())
                .and(exposeMap.REMOVED.isNull())
                .and(instanceHostMap.REMOVED.isNull())
                .and(host.STATE.notIn(CommonStatesConstants.REMOVING, CommonStatesConstants.REMOVED))
                .and(hostIpAddress.REMOVED.isNull())
                .and(exposeMap.SERVICE_ID.in(linkedServicesIds))
                .fetch().map(mapper);
    }

    protected List<HostMetaData> getAllInstanceHostMetaDataForAccount(final Account account) {
        MultiRecordMapper<HostMetaData> mapper = new MultiRecordMapper<HostMetaData>() {
            @Override
            protected HostMetaData map(List<Object> input) {
                Host host = (Host) input.get(0);
                IpAddress hostIp = (IpAddress) input.get(1);
                HostMetaData data = new HostMetaData(hostIp.getAddress(), host, account);
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
                .and(host.ACCOUNT_ID.eq(account.getId()))
                .fetch().map(mapper);
    }

    @Override
    public Map<Long, List<HealthcheckInstanceHostMap>> getInstanceIdToHealthCheckers(Account account) {
        Map<Long, List<HealthcheckInstanceHostMap>> instanceIdToHealthCheckers = new HashMap<>();
        List<? extends HealthcheckInstanceHostMap> hostMaps = objMgr.find(HealthcheckInstanceHostMap.class,
                HEALTHCHECK_INSTANCE_HOST_MAP.ACCOUNT_ID, account.getId(), HEALTHCHECK_INSTANCE_HOST_MAP.REMOVED, null);

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


    @Override
    public void fetchNetworks(final MetaHelperInfo helperInfo, final OutputStream os) {
        create()
                .select(NETWORK.NAME, NETWORK.UUID, NETWORK.ACCOUNT_ID, NETWORK.ID, NETWORK.DATA)
                .from(NETWORK)
                .where(NETWORK.REMOVED.isNull())
                .and(NETWORK.ACCOUNT_ID.eq(helperInfo.getAccount().getId()))
                .fetchInto(new RecordHandler<Record5<String, String, Long, Long, Map<String, Object>>>() {
                    @Override
                    public void next(Record5<String, String, Long, Long, Map<String, Object>> record) {
                        fetchNetwork(helperInfo, os, record);
                    }
                });

        if (!helperInfo.getOtherAccountsServicesIds().isEmpty()) {
            create()
                    .select(NETWORK.NAME, NETWORK.UUID, NETWORK.ACCOUNT_ID, NETWORK.ID, NETWORK.DATA)
                    .from(NETWORK)
                    .join(NIC)
                    .on(NIC.NETWORK_ID.eq(NETWORK.ID))
                    .join(SERVICE_EXPOSE_MAP)
                    .on(SERVICE_EXPOSE_MAP.INSTANCE_ID.eq(NIC.INSTANCE_ID))
                    .where(NETWORK.REMOVED.isNull())
                    .and(SERVICE_EXPOSE_MAP.REMOVED.isNull())
                    .and(SERVICE_EXPOSE_MAP.SERVICE_ID.in(helperInfo.getOtherAccountsServicesIds()))
                    .fetchInto(new RecordHandler<Record5<String, String, Long, Long, Map<String, Object>>>() {
                        @Override
                        public void next(Record5<String, String, Long, Long, Map<String, Object>> record) {
                            fetchNetwork(helperInfo, os, record);
                        }
                    });
        }

    }

    private void fetchNetwork(final MetaHelperInfo helperInfo, final OutputStream os,
            Record5<String, String, Long, Long, Map<String, Object>> record) {
        String name = record.getValue(NETWORK.NAME);
        String uuid = record.getValue(NETWORK.UUID);
        Long accountId = record.getValue(NETWORK.ACCOUNT_ID);
        Long id = record.getValue(NETWORK.ID);
        Map<String, Object> data = record.getValue(NETWORK.DATA);
        NetworkRecord ntwk = new NetworkRecord();
        ntwk.setData(data);
        Map<String, Object> meta = DataAccessor.fieldMap(ntwk, ServiceConstants.FIELD_METADATA);
        Account account = helperInfo.getAccounts().get(accountId);
        boolean isDefault = account.getDefaultNetworkId() == null ? false : account
                .getDefaultNetworkId().equals(id);
        boolean host_ports = DataAccessor.fieldBool(ntwk, NetworkConstants.FIELD_HOST_PORTS);
        Object policy = DataAccessor.field(ntwk, NetworkConstants.FIELD_POLICY, Object.class);
        String dpa = DataAccessor.fieldString(ntwk, NetworkConstants.FIELD_DEFAULT_POLICY_ACTION);
        NetworkMetaData ntwkMeta = new NetworkMetaData(name, uuid, host_ports, isDefault, meta, dpa, policy);
        writeToJson(os, ntwkMeta);
    }

    @Override
    public void fetchContainerLinks(MetaHelperInfo helperInfo, final OutputStream os) {
        final InstanceTable targetInstance = INSTANCE.as("target_instance");
        create()
                .select(INSTANCE_LINK.LINK_NAME, INSTANCE.UUID, targetInstance.UUID)
                .from(INSTANCE_LINK)
                .join(INSTANCE)
                .on(INSTANCE_LINK.INSTANCE_ID.eq(INSTANCE.ID))
                .join(targetInstance)
                .on(INSTANCE_LINK.TARGET_INSTANCE_ID.eq(targetInstance.ID))
                .where(INSTANCE.REMOVED.isNull())
                .and(INSTANCE_LINK.REMOVED.isNull())
                .and(targetInstance.REMOVED.isNull())
                .and(INSTANCE_LINK.ACCOUNT_ID.eq(helperInfo.getAccount().getId()))
                .fetchInto(new RecordHandler<Record3<String, String, String>>() {
                    @Override
                    public void next(Record3<String, String, String> record) {
                        String linkName = record.getValue(INSTANCE_LINK.LINK_NAME);
                        String instanceUUID = record.getValue(INSTANCE.UUID);
                        String targetInstanceUUID = record.getValue(targetInstance.UUID);
                        ContainerLinkMetaData data = new ContainerLinkMetaData(instanceUUID,
                                targetInstanceUUID,
                                linkName);
                        writeToJson(os, data);
                    }
                });
}


    private Condition getMultiAccountInstanceSearchCondition(final MetaHelperInfo helperInfo) {
        Condition condition = INSTANCE.ACCOUNT_ID.eq(helperInfo.getAccount().getId());
        if (!helperInfo.getOtherAccountsServicesIds().isEmpty()) {
            condition = INSTANCE.ACCOUNT_ID.eq(helperInfo.getAccount().getId()).or(
                    SERVICE_EXPOSE_MAP.SERVICE_ID.in(helperInfo.getOtherAccountsServicesIds()));
        }
        return condition;
    }

    protected void writeToJson(OutputStream os, Object data) {
        try {
            jsonMapper.writeValue(os, data);
        } catch (Throwable t) {
            ExceptionUtils.rethrowExpectedRuntime(t);
        }
    }

    @Override
    public void fetchHosts(MetaHelperInfo helperInfo, OutputStream os) {
        for (HostMetaData host : helperInfo.getHostIdToHostMetadata().values()) {
            writeToJson(os, host);
        }
    }

    @Override
    public void fetchSelf(HostMetaData selfHost, String version, OutputStream os) {
        DefaultMetaData def = new DefaultMetaData(version, selfHost);
        writeToJson(os, def);
    }

    protected void fetchLaunchConfigInfo(final MetaHelperInfo helperInfo, final OutputStream os, Service service,
            String stackName, String stackUUID, String launchConfigName, List<String> launchConfigNames) {
        List<String> sidekicks = new ArrayList<>();
        for (String lc : launchConfigNames) {
            if (!lc.equalsIgnoreCase(ServiceConstants.PRIMARY_LAUNCH_CONFIG_NAME)) {
                sidekicks.add(lc);
            }
        }
        LBConfigMetadataStyle lbConfig = lbInfoDao.generateLBConfigMetadataStyle(service);
        Object hcO = null;
        if (service.getKind().equalsIgnoreCase(ServiceConstants.KIND_EXTERNAL_SERVICE)) {
            hcO = DataAccessor.field(service, InstanceConstants.FIELD_HEALTH_CHECK, Object.class);
        } else {
            hcO = ServiceUtil.getLaunchConfigObject(service, launchConfigName,
                    InstanceConstants.FIELD_HEALTH_CHECK);
        }

        InstanceHealthCheck hc = null;
        if (hcO != null) {
            hc = jsonMapper.convertValue(hcO, InstanceHealthCheck.class);
        }
        String name = launchConfigName;
        if (launchConfigName.equalsIgnoreCase(ServiceConstants.PRIMARY_LAUNCH_CONFIG_NAME)) {
            name = service.getName();
        }

        ServiceMetaData data = new ServiceMetaData(service, name, stackName, stackUUID, sidekicks,
                hc, lbConfig, helperInfo.getAccount());

        writeToJson(os, data);
    }

    @Override
    public void fetchServices(final MetaHelperInfo helperInfo, final OutputStream os) {
        Condition condition = SERVICE.ACCOUNT_ID.eq(helperInfo.getAccount().getId());
        if (!helperInfo.getOtherAccountsServicesIds().isEmpty()) {
            condition = SERVICE.ACCOUNT_ID.eq(helperInfo.getAccount().getId()).or(
                    SERVICE.ID.in(helperInfo.getOtherAccountsServicesIds()));
        }
        create()
                .select(SERVICE.UUID, SERVICE.NAME, SERVICE.STATE, SERVICE.CREATE_INDEX, SERVICE.KIND, SERVICE.SYSTEM,
                        SERVICE.DATA, SERVICE.ACCOUNT_ID, SERVICE.STACK_ID,
                        STACK.UUID, STACK.NAME)
                .from(SERVICE)
                .join(STACK)
                .on(SERVICE.STACK_ID.eq(STACK.ID))
                .where(STACK.REMOVED.isNull())
                .and(SERVICE.REMOVED.isNull())
                .and(condition)
                .fetchInto(
                        new RecordHandler<Record11<String, String, String, Long, String, Boolean, Map<String, Object>, Long, Long, String, String>>() {
                    @Override
                            public void next(
                                    Record11<String, String, String, Long, String, Boolean, Map<String, Object>, Long, Long, String, String> record) {
                                ServiceRecord service = new ServiceRecord();
                                service.setName(record.getValue(SERVICE.NAME));
                                service.setUuid(record.getValue(SERVICE.UUID));
                                service.setState(record.getValue(SERVICE.STATE));
                                service.setCreateIndex(record.getValue(SERVICE.CREATE_INDEX));
                                service.setSystem(record.getValue(SERVICE.SYSTEM));
                                service.setData(record.getValue(SERVICE.DATA));
                                service.setKind(record.getValue(SERVICE.KIND));
                                service.setAccountId(record.getValue(SERVICE.ACCOUNT_ID));
                                service.setStackId(record.getValue(SERVICE.STACK_ID));
                                String stackName = record.getValue(STACK.NAME);
                                String stackUUID = record.getValue(STACK.UUID);

                                List<String> launchConfigNames = ServiceUtil
                                        .getLaunchConfigNames(service);
                                if (launchConfigNames.isEmpty()) {
                                    launchConfigNames.add(ServiceConstants.PRIMARY_LAUNCH_CONFIG_NAME);
                                }

                                for (String launchConfigName : launchConfigNames) {
                                    fetchLaunchConfigInfo(helperInfo, os, service, stackName, stackUUID,
                                            launchConfigName,
                                            launchConfigNames);
                                }
                    }
                });
    }

    @Override
    public void fetchStacks(final MetaHelperInfo helperInfo, final OutputStream os) {
        Condition condition = STACK.ACCOUNT_ID.eq(helperInfo.getAccount().getId());
        if (!helperInfo.getOtherAccountsServicesIds().isEmpty()) {
            condition = STACK.ACCOUNT_ID.eq(helperInfo.getAccount().getId()).or(
                    STACK.ID.in(helperInfo.getOtherAccountsStackIds()));
        }
        create()
                .select(STACK.NAME, STACK.UUID, STACK.SYSTEM)
                .from(STACK)
                .where(STACK.REMOVED.isNull())
                .and(condition)
                .fetchInto(new RecordHandler<Record3<String, String, Boolean>>() {
                    @Override
                    public void next(Record3<String, String, Boolean> record) {
                        String name = record.getValue(STACK.NAME);
                        String uuid = record.getValue(STACK.UUID);
                        Boolean system = record.getValue(STACK.SYSTEM);
                        StackMetaData data = new StackMetaData(name, uuid, system, helperInfo.getAccount());
                        writeToJson(os, data);
                    }
                });
    }

    @Override
    public void fetchServiceLinks(final MetaHelperInfo helperInfo, final OutputStream os) {
        final ServiceTable consumedService = SERVICE.as("consumed_service");
        Condition condition = SERVICE_CONSUME_MAP.ACCOUNT_ID.eq(helperInfo.getAccount().getId());
        if (!helperInfo.getOtherAccountsServicesIds().isEmpty()) {
            condition = SERVICE_CONSUME_MAP.ACCOUNT_ID.eq(helperInfo.getAccount().getId()).or(
                    SERVICE_CONSUME_MAP.SERVICE_ID.in(helperInfo.getOtherAccountsServicesIds()));
        }
        create()
                .select(SERVICE_CONSUME_MAP.NAME, SERVICE.UUID, STACK.NAME, consumedService.NAME)
                .from(SERVICE_CONSUME_MAP)
                .join(SERVICE)
                .on(SERVICE_CONSUME_MAP.SERVICE_ID.eq(SERVICE.ID))
                .join(consumedService)
                .on(SERVICE_CONSUME_MAP.CONSUMED_SERVICE_ID.eq(consumedService.ID))
                .join(STACK)
                .on(STACK.ID.eq(consumedService.STACK_ID))
                .where(SERVICE_CONSUME_MAP.REMOVED.isNull())
                .and(SERVICE.REMOVED.isNull())
                .and(consumedService.REMOVED.isNull())
                .and(STACK.REMOVED.isNull())
                .and(condition)
                .fetchInto(new RecordHandler<Record4<String, String, String, String>>() {
                    @Override
                    public void next(Record4<String, String, String, String> record) {
                        String consumeMapName = record.getValue(SERVICE_CONSUME_MAP.NAME);
                        String serviceUUID = record.getValue(SERVICE.UUID);
                        String stackName = record.getValue(STACK.NAME);
                        String consumedServiceName = record.getValue(consumedService.NAME);
                        String linkAlias = !StringUtils.isEmpty(consumeMapName) ? consumeMapName : consumedServiceName;

                        ServiceLinkMetaData data = new ServiceLinkMetaData(serviceUUID,
                                consumedServiceName,
                                stackName, linkAlias);
                        writeToJson(os, data);
                    }
                });
    }

    @Override
    public void fetchServiceContainerLinks(final MetaHelperInfo helperInfo, final OutputStream os) {
        Condition condition = SERVICE_EXPOSE_MAP.ACCOUNT_ID.eq(helperInfo.getAccount().getId());
        if (!helperInfo.getOtherAccountsServicesIds().isEmpty()) {
            condition = SERVICE_EXPOSE_MAP.ACCOUNT_ID.eq(helperInfo.getAccount().getId()).or(
                    SERVICE_EXPOSE_MAP.SERVICE_ID.in(helperInfo.getOtherAccountsServicesIds()));
        }
        create()
                .select(SERVICE_EXPOSE_MAP.DNS_PREFIX, INSTANCE.UUID, SERVICE.UUID, SERVICE.NAME)
                .from(SERVICE_EXPOSE_MAP)
                .join(INSTANCE)
                .on(INSTANCE.ID.eq(SERVICE_EXPOSE_MAP.INSTANCE_ID))
                .join(SERVICE)
                .on(SERVICE_EXPOSE_MAP.SERVICE_ID.eq(SERVICE.ID))
                .where(SERVICE_EXPOSE_MAP.REMOVED.isNull())
                .and(SERVICE.REMOVED.isNull())
                .and(INSTANCE.REMOVED.isNull())
                .and(condition)
                .fetchInto(new RecordHandler<Record4<String, String, String, String>>() {
                    @Override
                    public void next(Record4<String, String, String, String> record) {
                        String dnsPrefix = record.getValue(SERVICE_EXPOSE_MAP.DNS_PREFIX);
                        String instanceUUID = record.getValue(INSTANCE.UUID);
                        String serviceName = record.getValue(SERVICE.NAME);
                        String serviceUUID = record.getValue(SERVICE.UUID);
                        String svcName = dnsPrefix != null ? dnsPrefix : serviceName;
                        ServiceContainerLinkMetaData data = new ServiceContainerLinkMetaData(serviceUUID, svcName,
                                instanceUUID);
                        writeToJson(os, data);
                    }
                });
    }
}