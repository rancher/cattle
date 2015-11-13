package io.cattle.platform.configitem.context.dao.impl;

import static io.cattle.platform.core.model.tables.HostIpAddressMapTable.HOST_IP_ADDRESS_MAP;
import static io.cattle.platform.core.model.tables.InstanceHostMapTable.INSTANCE_HOST_MAP;
import static io.cattle.platform.core.model.tables.InstanceLinkTable.INSTANCE_LINK;
import static io.cattle.platform.core.model.tables.InstanceTable.INSTANCE;
import static io.cattle.platform.core.model.tables.IpAddressNicMapTable.IP_ADDRESS_NIC_MAP;
import static io.cattle.platform.core.model.tables.IpAddressTable.IP_ADDRESS;
import static io.cattle.platform.core.model.tables.NetworkTable.NETWORK;
import static io.cattle.platform.core.model.tables.NicTable.NIC;
import static io.cattle.platform.core.model.tables.ServiceConsumeMapTable.SERVICE_CONSUME_MAP;
import static io.cattle.platform.core.model.tables.ServiceExposeMapTable.SERVICE_EXPOSE_MAP;
import static io.cattle.platform.core.model.tables.ServiceTable.SERVICE;
import io.cattle.platform.configitem.context.dao.DnsInfoDao;
import io.cattle.platform.configitem.context.data.DnsEntryData;
import io.cattle.platform.configitem.context.data.DnsResolveEntryData;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.HealthcheckConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.IpAddressConstants;
import io.cattle.platform.core.dao.NetworkDao;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.InstanceHostMap;
import io.cattle.platform.core.model.InstanceLink;
import io.cattle.platform.core.model.IpAddress;
import io.cattle.platform.core.model.Network;
import io.cattle.platform.core.model.Nic;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.ServiceConsumeMap;
import io.cattle.platform.core.model.ServiceExposeMap;
import io.cattle.platform.core.model.tables.InstanceHostMapTable;
import io.cattle.platform.core.model.tables.InstanceLinkTable;
import io.cattle.platform.core.model.tables.InstanceTable;
import io.cattle.platform.core.model.tables.IpAddressNicMapTable;
import io.cattle.platform.core.model.tables.IpAddressTable;
import io.cattle.platform.core.model.tables.NicTable;
import io.cattle.platform.core.model.tables.ServiceConsumeMapTable;
import io.cattle.platform.core.model.tables.ServiceExposeMapTable;
import io.cattle.platform.core.model.tables.ServiceTable;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.db.jooq.mapper.MultiRecordMapper;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.servicediscovery.api.constants.ServiceDiscoveryConstants;
import io.cattle.platform.servicediscovery.service.ServiceDiscoveryService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.jooq.Condition;

public class DnsInfoDaoImpl extends AbstractJooqDao implements DnsInfoDao {

    @Inject
    NetworkDao networkDao;

    @Inject
    ObjectManager objManager;

    @Inject
    ServiceDiscoveryService sdService;

    @Override
    public List<DnsEntryData> getInstanceLinksHostDnsData(final Instance instance) {
        MultiRecordMapper<DnsEntryData> mapper = new MultiRecordMapper<DnsEntryData>() {
            @Override
            protected DnsEntryData map(List<Object> input) {
                DnsEntryData data = new DnsEntryData();
                Map<String, Map<String, String>> resolve = new HashMap<>();
                Map<String, String> ips = new HashMap<>();
                String targetInstanceName = input.get(4) == null ? null : ((Instance) input.get(4)).getName();
                ips.put(((IpAddress) input.get(1)).getAddress(), targetInstanceName);
                // add all instance links
                resolve.put(((InstanceLink) input.get(0)).getLinkName(), ips);
                data.setSourceIpAddress((IpAddress) input.get(2));
                data.setResolveServicesAndContainers(resolve);
                data.setInstance((Instance)input.get(3));
                return data;
            }
        };

        InstanceLinkTable instanceLink = mapper.add(INSTANCE_LINK);
        IpAddressTable targetIpAddress = mapper.add(IP_ADDRESS);
        IpAddressTable clientIpAddress = mapper.add(IP_ADDRESS);
        InstanceTable clientInstance = mapper.add(INSTANCE);
        InstanceTable targetInstance = mapper.add(INSTANCE);
        NicTable clientNic = NIC.as("client_nic");
        NicTable targetNic = NIC.as("target_nic");
        IpAddressNicMapTable clientNicIpTable = IP_ADDRESS_NIC_MAP.as("client_nic_ip");
        IpAddressNicMapTable targetNicIpTable = IP_ADDRESS_NIC_MAP.as("target_nic_ip");

        return create()
                .select(mapper.fields())
                .from(NIC)
                .join(clientNic)
                .on(NIC.VNET_ID.eq(clientNic.VNET_ID))
                .join(instanceLink)
                .on(instanceLink.INSTANCE_ID.eq(clientNic.INSTANCE_ID))
                .join(targetNic)
                .on(targetNic.INSTANCE_ID.eq(instanceLink.TARGET_INSTANCE_ID))
                .join(targetInstance)
                .on(targetNic.INSTANCE_ID.eq(targetInstance.ID))
                .join(targetNicIpTable)
                .on(targetNicIpTable.NIC_ID.eq(targetNic.ID))
                .join(targetIpAddress)
                .on(targetNicIpTable.IP_ADDRESS_ID.eq(targetIpAddress.ID))
                .join(clientNicIpTable)
                .on(clientNicIpTable.NIC_ID.eq(clientNic.ID))
                .join(clientIpAddress)
                .on(clientNicIpTable.IP_ADDRESS_ID.eq(clientIpAddress.ID))
                .join(clientInstance)
                .on(clientNic.INSTANCE_ID.eq(clientInstance.ID))
                .where(NIC.INSTANCE_ID.eq(instance.getId())
                        .and(NIC.VNET_ID.isNotNull())
                        .and(NIC.REMOVED.isNull())
                        .and(targetIpAddress.ROLE.eq(IpAddressConstants.ROLE_PRIMARY))
                        .and(targetIpAddress.REMOVED.isNull())
                        .and(clientIpAddress.ROLE.eq(IpAddressConstants.ROLE_PRIMARY))
                        .and(clientIpAddress.REMOVED.isNull())
                        .and(targetNicIpTable.REMOVED.isNull())
                        .and(clientNic.REMOVED.isNull())
                        .and(targetNic.REMOVED.isNull())
                        .and(instanceLink.REMOVED.isNull())
                        .and(instanceLink.SERVICE_CONSUME_MAP_ID.isNull())
                        .and(targetInstance.STATE.in(InstanceConstants.STATE_RUNNING,
                                InstanceConstants.STATE_STARTING))
                        .and(targetInstance.HEALTH_STATE.isNull().or(
                                targetInstance.HEALTH_STATE.eq(HealthcheckConstants.HEALTH_STATE_HEALTHY))))
                .fetch().map(mapper);
    }

    @Override
    public List<DnsEntryData> getServiceDnsData(final Instance instance, final boolean isVIPProvider, boolean links) {
        final Map<Long, IpAddress> instanceIdToHostIpMap = getInstanceWithHostNetworkingToIpMap(instance.getAccountId());
        MultiRecordMapper<DnsEntryData> mapper = new MultiRecordMapper<DnsEntryData>() {
            @Override
            protected DnsEntryData map(List<Object> input) {
                DnsEntryData data = new DnsEntryData();
                Service service = (Service) input.get(0);
                Map<String, Map<String, String>> resolve = new HashMap<>();
                Map<String, String> ips = new HashMap<>();
                IpAddress sourceIp = getIpAddress((IpAddress) input.get(2), (Nic) input.get(7), true, instanceIdToHostIpMap);
                String targetInstanceName = input.get(8) == null ? null : ((Instance) input.get(8)).getName();
                if (isVIPProvider) {
                    ips.put(service.getVip(), targetInstanceName);
                } else {
                    IpAddress targetIp = getIpAddress((IpAddress) input.get(1), (Nic) input.get(6), false,
                            instanceIdToHostIpMap);
                    ips.put(targetIp.getAddress(), targetInstanceName);
                }
                String dnsName = getDnsName(service, input.get(4), input.get(5), false);
                data.setSourceIpAddress(sourceIp);
                resolve.put(dnsName, ips);
                data.setResolveServicesAndContainers(resolve);
                data.setInstance((Instance) input.get(3));
                return data;
            }
        };

        ServiceTable targetService = mapper.add(SERVICE);
        IpAddressTable targetIpAddress = mapper.add(IP_ADDRESS);
        IpAddressTable clientIpAddress = mapper.add(IP_ADDRESS);
        InstanceTable clientInstance = mapper.add(INSTANCE);
        ServiceConsumeMapTable serviceConsumeMap = mapper.add(SERVICE_CONSUME_MAP);
        ServiceExposeMapTable targetServiceExposeMap = mapper.add(SERVICE_EXPOSE_MAP);
        NicTable targetNic = mapper.add(NIC);
        NicTable clientNic = mapper.add(NIC);
        InstanceTable targetInstance = mapper.add(INSTANCE);
        IpAddressNicMapTable clientNicIpTable = IP_ADDRESS_NIC_MAP.as("client_nic_ip");
        IpAddressNicMapTable targetNicIpTable = IP_ADDRESS_NIC_MAP.as("target_nic_ip");
        ServiceExposeMapTable clientServiceExposeMap = SERVICE_EXPOSE_MAP.as("service_expose_map_client");
        ServiceTable clientService = SERVICE.as("client_service");

        Condition condition = null;
        if (links) {
            condition = serviceConsumeMap.ID.isNotNull().and(serviceConsumeMap.REMOVED.isNull())
                    .and(serviceConsumeMap.STATE.in(CommonStatesConstants.ACTIVATING,
                            CommonStatesConstants.ACTIVE));
        } else {
            condition = targetService.ENVIRONMENT_ID.eq(clientService.ENVIRONMENT_ID).and(serviceConsumeMap.ID.isNull())
                    .and(clientService.ID.ne(targetService.ID));
        }

        return create()
                .select(mapper.fields())
                .from(NIC)
                .join(clientNic)
                .on(NIC.VNET_ID.eq(clientNic.VNET_ID))
                .join(clientServiceExposeMap)
                .on(clientServiceExposeMap.INSTANCE_ID.eq(clientNic.INSTANCE_ID))
                .join(clientService)
                .on(clientService.ID.eq(clientServiceExposeMap.SERVICE_ID))
                .join(targetService)
                .on(targetService.ACCOUNT_ID.eq(clientServiceExposeMap.ACCOUNT_ID))
                .join(targetServiceExposeMap)
                .on(targetServiceExposeMap.SERVICE_ID.eq(targetService.ID))
                .join(targetNic)
                .on(targetNic.INSTANCE_ID.eq(targetServiceExposeMap.INSTANCE_ID))
                .join(targetInstance)
                .on(targetNic.INSTANCE_ID.eq(targetInstance.ID))
                .join(targetNicIpTable)
                .on(targetNicIpTable.NIC_ID.eq(targetNic.ID))
                .join(targetIpAddress)
                .on(targetNicIpTable.IP_ADDRESS_ID.eq(targetIpAddress.ID))
                .join(clientNicIpTable)
                .on(clientNicIpTable.NIC_ID.eq(clientNic.ID))
                .join(clientIpAddress)
                .on(clientNicIpTable.IP_ADDRESS_ID.eq(clientIpAddress.ID))
                .join(clientInstance)
                .on(clientNic.INSTANCE_ID.eq(clientInstance.ID))
                .leftOuterJoin(serviceConsumeMap)
                .on(serviceConsumeMap.SERVICE_ID.eq(clientService.ID).and(
                        serviceConsumeMap.CONSUMED_SERVICE_ID.eq(targetService.ID))
                        .and(serviceConsumeMap.REMOVED.isNull()))
                .where(NIC.INSTANCE_ID.eq(instance.getId())
                        .and(NIC.VNET_ID.isNotNull())
                        .and(NIC.REMOVED.isNull())
                        .and(targetIpAddress.ROLE.eq(IpAddressConstants.ROLE_PRIMARY))
                        .and(targetIpAddress.REMOVED.isNull())
                        .and(clientIpAddress.ROLE.eq(IpAddressConstants.ROLE_PRIMARY))
                        .and(clientIpAddress.REMOVED.isNull())
                        .and(targetNicIpTable.REMOVED.isNull())
                        .and(clientNic.REMOVED.isNull())
                        .and(targetNic.REMOVED.isNull())
                        .and(clientServiceExposeMap.REMOVED.isNull())
                        .and(targetServiceExposeMap.REMOVED.isNull())
                        .and(targetService.REMOVED.isNull())
                        .and(clientService.REMOVED.isNull())
                        .and(targetInstance.STATE.in(InstanceConstants.STATE_RUNNING,
                                InstanceConstants.STATE_STARTING))
                        .and(targetInstance.HEALTH_STATE.isNull().or(
                                targetInstance.HEALTH_STATE.eq(HealthcheckConstants.HEALTH_STATE_HEALTHY)))
                        .and(condition))
                .fetch().map(mapper);
    }

    @Override
    public List<DnsEntryData> getSelfServiceData(Instance instance, final boolean isVIPProvider) {
        final Map<Long, IpAddress> instanceIdToHostIpMap = getInstanceWithHostNetworkingToIpMap(instance.getAccountId());
        // each dnsEntry data represents a client ip + resolve
        // fetching only client data here
        List<DnsEntryData> dnsRecords = getClientData(instance, instanceIdToHostIpMap);
        Map<Long, List<DnsEntryData>> serviceIdToDnsRecordsData = getServiceIdToDnsRecordData(dnsRecords);

        // each resolve represents DNS name + IP
        List<DnsResolveEntryData> resolveRecords = getTargetData(instance, isVIPProvider, instanceIdToHostIpMap,
                serviceIdToDnsRecordsData.keySet());

        // Map resolve to client
        for (DnsResolveEntryData resolve : resolveRecords) {
            addResolveToDnsRecord(serviceIdToDnsRecordsData, resolve);
        }

        return dnsRecords;
    }

    protected void addResolveToDnsRecord(Map<Long, List<DnsEntryData>> serviceIdToDnsRecordsData,
            DnsResolveEntryData resolve) {
        List<DnsEntryData> dnsRecords = serviceIdToDnsRecordsData.get(resolve.getServiceId());
        for (DnsEntryData dnsRecord : dnsRecords) {
            Map<String, Map<String, String>> recordResolve = dnsRecord.getResolveServicesAndContainers();
            Map<String, String> ips = recordResolve.get(resolve.getDnsName());
            if (ips == null) {
                ips = new HashMap<>();
            }
            ips.put(resolve.getIpAddress(), resolve.getTargetInstanceName());
            recordResolve.put(resolve.getDnsName(), ips);
            dnsRecord.setResolveServicesAndContainers(recordResolve);
        }
    }

    protected Map<Long, List<DnsEntryData>> getServiceIdToDnsRecordData(List<DnsEntryData> dnsRecordData) {
        Map<Long, List<DnsEntryData>> serviceIdToDnsRecordData = new HashMap<>();
        for (DnsEntryData dnsRecord : dnsRecordData) {
            List<DnsEntryData> clientIps = serviceIdToDnsRecordData.get(dnsRecord.getClientServiceId());
            if (clientIps == null) {
                clientIps = new ArrayList<>();
            }
            clientIps.add(dnsRecord);
            serviceIdToDnsRecordData.put(dnsRecord.getClientServiceId(), clientIps);
        }
        return serviceIdToDnsRecordData;
    }

    protected List<DnsResolveEntryData> getTargetData(Instance instance, final boolean isVIPProvider,
            final Map<Long, IpAddress> instanceIdToHostIpMap, Set<Long> serviceIds) {
        MultiRecordMapper<DnsResolveEntryData> resolveMapper = new MultiRecordMapper<DnsResolveEntryData>() {
            @Override
            protected DnsResolveEntryData map(List<Object> input) {
                DnsResolveEntryData resolveData = new DnsResolveEntryData();
                // target info
                Service service = (Service) input.get(0);
                String dnsName = getDnsName(service, null, input.get(2), true);
                String targetIp = null;
                if (isVIPProvider) {
                    targetIp = service.getVip();
                } else {
                    targetIp = getIpAddress((IpAddress) input.get(1), (Nic) input.get(3), false,
                            instanceIdToHostIpMap).getAddress();
                }
                resolveData.setDnsName(dnsName);
                resolveData.setIpAddress(targetIp);
                resolveData.setServiceId(service.getId());
                resolveData.setTargetInstanceName(((Instance) input.get(4)).getName());
                return resolveData;
            }
        };

        ServiceTable targetService = resolveMapper.add(SERVICE);
        IpAddressTable targetIpAddress = resolveMapper.add(IP_ADDRESS);
        ServiceExposeMapTable targetServiceExposeMap = resolveMapper.add(SERVICE_EXPOSE_MAP);
        NicTable targetNic = resolveMapper.add(NIC);
        InstanceTable targetInstance = resolveMapper.add(INSTANCE);
        IpAddressNicMapTable targetNicIpTable = IP_ADDRESS_NIC_MAP.as("target_nic_ip");

        List<DnsResolveEntryData> resolveData = create()
                .select(resolveMapper.fields())
                .from(targetService)
                .join(targetServiceExposeMap)
                .on(targetServiceExposeMap.SERVICE_ID.eq(targetService.ID))
                .join(targetNic)
                .on(targetNic.INSTANCE_ID.eq(targetServiceExposeMap.INSTANCE_ID))
                .join(targetInstance)
                .on(targetNic.INSTANCE_ID.eq(targetInstance.ID))
                .join(targetNicIpTable)
                .on(targetNicIpTable.NIC_ID.eq(targetNic.ID))
                .join(targetIpAddress)
                .on(targetNicIpTable.IP_ADDRESS_ID.eq(targetIpAddress.ID))
                .where(targetIpAddress.ROLE.eq(IpAddressConstants.ROLE_PRIMARY))
                .and(targetIpAddress.REMOVED.isNull())
                .and(targetNicIpTable.REMOVED.isNull())
                .and(targetNic.REMOVED.isNull())
                .and(targetServiceExposeMap.REMOVED.isNull())
                .and(targetInstance.STATE.in(InstanceConstants.STATE_RUNNING,
                        InstanceConstants.STATE_STARTING))
                .and(targetService.ID.in(serviceIds))
                .and(targetInstance.HEALTH_STATE.isNull().or(
                        targetInstance.HEALTH_STATE.eq(HealthcheckConstants.HEALTH_STATE_HEALTHY)))
                .fetch().map(resolveMapper);

        return resolveData;
    }

    protected List<DnsEntryData> getClientData(Instance instance, final Map<Long, IpAddress> instanceIdToHostIpMap) {
        MultiRecordMapper<DnsEntryData> mapper = new MultiRecordMapper<DnsEntryData>() {
            @Override
            protected DnsEntryData map(List<Object> input) {
                DnsEntryData data = new DnsEntryData();
                IpAddress sourceIp = getIpAddress((IpAddress) input.get(0), (Nic) input.get(2), true,
                        instanceIdToHostIpMap);
                data.setSourceIpAddress(sourceIp);
                data.setInstance((Instance) input.get(1));
                Service service = (Service) input.get(3);
                data.setClientServiceId(service.getId());
                return data;
            }
        };
        IpAddressTable clientIpAddress = mapper.add(IP_ADDRESS);
        InstanceTable clientInstance = mapper.add(INSTANCE);
        NicTable clientNic = mapper.add(NIC);
        ServiceTable clientService = mapper.add(SERVICE);
        IpAddressNicMapTable clientNicIpTable = IP_ADDRESS_NIC_MAP.as("client_nic_ip");
        ServiceExposeMapTable clientServiceExposeMap = SERVICE_EXPOSE_MAP.as("service_expose_map_client");

        List<DnsEntryData> dnsData = create()
                .select(mapper.fields())
                .from(NIC)
                .join(clientNic)
                .on(NIC.VNET_ID.eq(clientNic.VNET_ID))
                .join(clientServiceExposeMap)
                .on(clientServiceExposeMap.INSTANCE_ID.eq(clientNic.INSTANCE_ID))
                .join(clientService)
                .on(clientService.ID.eq(clientServiceExposeMap.SERVICE_ID))
                .join(clientNicIpTable)
                .on(clientNicIpTable.NIC_ID.eq(clientNic.ID))
                .join(clientIpAddress)
                .on(clientNicIpTable.IP_ADDRESS_ID.eq(clientIpAddress.ID))
                .join(clientInstance)
                .on(clientNic.INSTANCE_ID.eq(clientInstance.ID))
                .where(NIC.INSTANCE_ID.eq(instance.getId())
                        .and(NIC.VNET_ID.isNotNull())
                        .and(NIC.REMOVED.isNull())
                        .and(clientIpAddress.ROLE.eq(IpAddressConstants.ROLE_PRIMARY))
                        .and(clientIpAddress.REMOVED.isNull())
                        .and(clientNic.REMOVED.isNull())
                        .and(clientServiceExposeMap.REMOVED.isNull()))
                .fetch().map(mapper);
        return dnsData;
    }


    @Override
    public List<DnsEntryData> getExternalServiceDnsData(final Instance instance, boolean links) {
        MultiRecordMapper<DnsEntryData> mapper = new MultiRecordMapper<DnsEntryData>() {
            @Override
            protected DnsEntryData map(List<Object> input) {
                DnsEntryData data = new DnsEntryData();
                ServiceExposeMap exposeMap = (ServiceExposeMap) input.get(1);
                if (exposeMap.getIpAddress() != null) {
                    Map<String, Map<String, String>> resolve = new HashMap<>();
                    Map<String, String> ips = new HashMap<>();
                    ips.put(exposeMap.getIpAddress(), null);
                    resolve.put(getDnsName(input.get(0), input.get(4), exposeMap, false), ips);
                    data.setResolveServicesAndContainers(resolve);
                } else if (exposeMap.getHostName() != null) {
                    Map<String, String> resolveHostName = new HashMap<>();
                    resolveHostName.put(getDnsName(input.get(0), input.get(4), exposeMap, false),
                            exposeMap.getHostName());
                    data.setResolveCname(resolveHostName);
                }

                data.setSourceIpAddress((IpAddress) input.get(2));
                data.setInstance((Instance) input.get(3));
                return data;
            }
        };

        ServiceTable targetService = mapper.add(SERVICE);
        ServiceExposeMapTable targetServiceExposeMap = mapper.add(SERVICE_EXPOSE_MAP);
        IpAddressTable clientIpAddress = mapper.add(IP_ADDRESS);
        InstanceTable clientInstance = mapper.add(INSTANCE);
        ServiceConsumeMapTable serviceConsumeMap = mapper.add(SERVICE_CONSUME_MAP);
        NicTable clientNic = NIC.as("client_nic");
        IpAddressNicMapTable clientNicIpTable = IP_ADDRESS_NIC_MAP.as("client_nic_ip");
        ServiceExposeMapTable clientServiceExposeMap = SERVICE_EXPOSE_MAP.as("service_expose_map_client");
        ServiceTable clientService = SERVICE.as("client_service");

        Condition condition = null;
        if (links) {
            condition = serviceConsumeMap.ID.isNotNull().and(serviceConsumeMap.REMOVED.isNull())
                    .and(serviceConsumeMap.STATE.in(CommonStatesConstants.ACTIVATING,
                            CommonStatesConstants.ACTIVE));
        } else {
            condition = targetService.ENVIRONMENT_ID.eq(clientService.ENVIRONMENT_ID)
                    .and(serviceConsumeMap.ID.isNull())
                    .and(clientService.ID.ne(targetService.ID));
        }

        return create()
                .select(mapper.fields())
                .from(NIC)
                .join(clientNic)
                .on(NIC.VNET_ID.eq(clientNic.VNET_ID))
                .join(clientNicIpTable)
                .on(clientNicIpTable.NIC_ID.eq(clientNic.ID))
                .join(clientIpAddress)
                .on(clientNicIpTable.IP_ADDRESS_ID.eq(clientIpAddress.ID))
                .join(clientInstance)
                .on(clientNic.INSTANCE_ID.eq(clientInstance.ID))
                .join(clientServiceExposeMap)
                .on(clientServiceExposeMap.INSTANCE_ID.eq(clientInstance.ID))
                .join(clientService)
                .on(clientService.ID.eq(clientServiceExposeMap.SERVICE_ID))
                .join(targetService)
                .on(targetService.ACCOUNT_ID.eq(clientService.ACCOUNT_ID))
                .join(targetServiceExposeMap)
                .on(targetServiceExposeMap.SERVICE_ID.eq(targetService.ID))
                .leftOuterJoin(serviceConsumeMap)
                .on(serviceConsumeMap.SERVICE_ID.eq(clientService.ID).and(
                        serviceConsumeMap.CONSUMED_SERVICE_ID.eq(targetService.ID))
                        .and(serviceConsumeMap.REMOVED.isNull()))
                .where(NIC.INSTANCE_ID.eq(instance.getId())
                        .and(NIC.VNET_ID.isNotNull())
                        .and(NIC.REMOVED.isNull())
                        .and(clientIpAddress.ROLE.eq(IpAddressConstants.ROLE_PRIMARY))
                        .and(clientIpAddress.REMOVED.isNull())
                        .and(clientNic.REMOVED.isNull())
                        .and(clientServiceExposeMap.REMOVED.isNull())
                        .and(targetServiceExposeMap.REMOVED.isNull())
                        .and(targetService.REMOVED.isNull())
                        .and(targetServiceExposeMap.IP_ADDRESS.isNotNull().or(
                                targetServiceExposeMap.HOST_NAME.isNotNull()))
                        .and(condition))
                .fetch().map(mapper);
    }


    @Override
    public List<DnsEntryData> getDnsServiceLinksData(Instance instance, final boolean isVIPProvider, boolean links) {
        final Map<Long, IpAddress> instanceIdToHostIpMap = getInstanceWithHostNetworkingToIpMap(instance.getAccountId());
        MultiRecordMapper<DnsEntryData> mapper = new MultiRecordMapper<DnsEntryData>() {
            @Override
            protected DnsEntryData map(List<Object> input) {
                DnsEntryData data = new DnsEntryData();
                Map<String, Map<String, String>> resolve = new HashMap<>();
                IpAddress sourceIp = getIpAddress((IpAddress) input.get(2), (Nic) input.get(7), true,
                        instanceIdToHostIpMap);
                Map<String, String> ips = new HashMap<>();
                if (isVIPProvider) {
                    Service targetService = (Service) input.get(6);
                    ips.put(targetService.getVip(), null);
                } else {
                    IpAddress targetIp = getIpAddress((IpAddress) input.get(1), (Nic) input.get(8), false,
                            instanceIdToHostIpMap);
                    ips.put(targetIp.getAddress(), null);
                }
                resolve.put(getDnsName(input.get(0), input.get(4), input.get(5), false), ips);
                data.setSourceIpAddress(sourceIp);
                data.setResolveServicesAndContainers(resolve);
                data.setInstance((Instance) input.get(3));
                return data;
            }
        };

        ServiceTable dnsService = mapper.add(SERVICE);
        IpAddressTable targetIpAddress = mapper.add(IP_ADDRESS);
        IpAddressTable clientIpAddress = mapper.add(IP_ADDRESS);
        InstanceTable clientInstance = mapper.add(INSTANCE);
        ServiceConsumeMapTable dnsConsumeMap = mapper.add(SERVICE_CONSUME_MAP);
        ServiceExposeMapTable targetServiceExposeMap = mapper.add(SERVICE_EXPOSE_MAP);
        ServiceTable consumedService = mapper.add(SERVICE);
        NicTable clientNic = mapper.add(NIC);
        NicTable targetNic = mapper.add(NIC);
        InstanceTable targetInstance = INSTANCE.as("instance");
        IpAddressNicMapTable clientNicIpTable = IP_ADDRESS_NIC_MAP.as("client_nic_ip");
        IpAddressNicMapTable targetNicIpTable = IP_ADDRESS_NIC_MAP.as("target_nic_ip");
        ServiceExposeMapTable clientServiceExposeMap = SERVICE_EXPOSE_MAP.as("service_expose_map_client");
        ServiceConsumeMapTable serviceConsumeMap = SERVICE_CONSUME_MAP.as("service_consume_map");
        ServiceTable clientService = SERVICE.as("client_service");

        Condition condition = null;
        if (links) {
            condition = dnsConsumeMap.ID.isNotNull().and(dnsConsumeMap.REMOVED.isNull())
                    .and(dnsConsumeMap.STATE.in(CommonStatesConstants.ACTIVATING,
                            CommonStatesConstants.ACTIVE));
        } else {
            condition = dnsService.ENVIRONMENT_ID.eq(clientService.ENVIRONMENT_ID)
                    .and(dnsConsumeMap.ID.isNull())
                    .and(clientService.ID.ne(dnsService.ID));
        }

        return create()
                .select(mapper.fields())
                .from(NIC)
                .join(clientNic)
                .on(NIC.VNET_ID.eq(clientNic.VNET_ID))
                .join(clientNicIpTable)
                .on(clientNicIpTable.NIC_ID.eq(clientNic.ID))
                .join(clientIpAddress)
                .on(clientNicIpTable.IP_ADDRESS_ID.eq(clientIpAddress.ID))
                .join(clientInstance)
                .on(clientNic.INSTANCE_ID.eq(clientInstance.ID))
                .join(clientServiceExposeMap)
                .on(clientServiceExposeMap.INSTANCE_ID.eq(clientInstance.ID))
                .join(clientService)
                .on(clientService.ID.eq(clientServiceExposeMap.SERVICE_ID))
                .join(dnsService)
                .on(dnsService.ACCOUNT_ID.eq(clientService.ACCOUNT_ID))
                .leftOuterJoin(dnsConsumeMap)
                .on(dnsConsumeMap.SERVICE_ID.eq(clientService.ID).and(
                        dnsConsumeMap.CONSUMED_SERVICE_ID.eq(dnsService.ID))
                        .and(dnsConsumeMap.REMOVED.isNull()))
                .join(serviceConsumeMap)
                .on(serviceConsumeMap.SERVICE_ID.eq(dnsService.ID))
                .join(consumedService)
                .on(consumedService.ID.eq(serviceConsumeMap.CONSUMED_SERVICE_ID))
                .join(targetServiceExposeMap)
                .on(targetServiceExposeMap.SERVICE_ID.eq(serviceConsumeMap.CONSUMED_SERVICE_ID))
                .join(targetNic)
                .on(targetNic.INSTANCE_ID.eq(targetServiceExposeMap.INSTANCE_ID))
                .join(targetInstance)
                .on(targetNic.INSTANCE_ID.eq(targetInstance.ID))
                .join(targetNicIpTable)
                .on(targetNicIpTable.NIC_ID.eq(targetNic.ID))
                .join(targetIpAddress)
                .on(targetNicIpTable.IP_ADDRESS_ID.eq(targetIpAddress.ID))
                .where(NIC.INSTANCE_ID.eq(instance.getId())
                        .and(NIC.VNET_ID.isNotNull())
                        .and(NIC.REMOVED.isNull())
                        .and(dnsService.KIND.eq(ServiceDiscoveryConstants.KIND.DNSSERVICE.name()))
                        .and(targetIpAddress.ROLE.eq(IpAddressConstants.ROLE_PRIMARY))
                        .and(targetIpAddress.REMOVED.isNull())
                        .and(clientIpAddress.ROLE.eq(IpAddressConstants.ROLE_PRIMARY))
                        .and(clientIpAddress.REMOVED.isNull())
                        .and(targetNicIpTable.REMOVED.isNull())
                        .and(clientNic.REMOVED.isNull())
                        .and(targetNic.REMOVED.isNull())
                        .and(serviceConsumeMap.REMOVED.isNull())
                        .and(serviceConsumeMap.STATE.in(CommonStatesConstants.ACTIVATING,
                                CommonStatesConstants.ACTIVE))
                        .and(clientServiceExposeMap.REMOVED.isNull())
                        .and(targetServiceExposeMap.REMOVED.isNull())
                        .and(targetInstance.STATE.in(InstanceConstants.STATE_RUNNING,
                                InstanceConstants.STATE_STARTING))
                        .and(dnsService.STATE.in(sdService.getServiceActiveStates(true)))
                        .and(targetInstance.HEALTH_STATE.isNull().or(
                                targetInstance.HEALTH_STATE.eq(HealthcheckConstants.HEALTH_STATE_HEALTHY)))
                        .and(condition))
                .fetch().map(mapper);
    }

    protected String getDnsName(Object service, Object serviceConsumeMap, Object serviceExposeMap, boolean self) {

        String dnsPrefix = null;
        if (serviceExposeMap != null) {
            dnsPrefix = ((ServiceExposeMap) serviceExposeMap).getDnsPrefix();
        }

        String consumeMapName = null;
        if (serviceConsumeMap != null) {
            consumeMapName = ((ServiceConsumeMap) serviceConsumeMap).getName();
        }

        String primaryDnsName = (consumeMapName != null && !consumeMapName.isEmpty()) ? consumeMapName
                : ((Service) service).getName();
        String dnsName = primaryDnsName;
        if (self) {
            dnsName = dnsPrefix == null ? dnsName : dnsPrefix;
        } else {
            dnsName = dnsPrefix == null ? dnsName : dnsPrefix + "." + dnsName;
        }

        return dnsName;
    }

    protected IpAddress getIpAddress(IpAddress ip, Nic nic, boolean isSource, Map<Long, IpAddress> instanceIdToHostIpMap) {
        if (nic.getDeviceNumber().equals(0)) {
            return ip;
        } else {
            IpAddress hostIp = instanceIdToHostIpMap.get(nic.getInstanceId());
            if (hostIp != null) {
                if (isSource) {
                    ip.setAddress("default");
                    return ip;
                }
                return hostIp;
            }
        }
        return null;
    }

    protected Map<Long, IpAddress> getInstanceWithHostNetworkingToIpMap(long accountId) {
        List<HostInstanceIpData> data = getHostContainerIpData(accountId);
        Map<Long, IpAddress> instanceIdToHostIpMap = new HashMap<>();
        for (HostInstanceIpData entry : data) {
            instanceIdToHostIpMap.put(entry.getInstanceHostMap().getInstanceId(), entry.getIpAddress());
        }

        return instanceIdToHostIpMap;
    }

    protected List<HostInstanceIpData> getHostContainerIpData(long accountId) {
        Network hostNtwk = objManager.findAny(Network.class, NETWORK.ACCOUNT_ID, accountId, NETWORK.REMOVED, null,
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
}
