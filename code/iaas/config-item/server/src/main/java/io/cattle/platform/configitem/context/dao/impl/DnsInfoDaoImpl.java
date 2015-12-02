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
import io.cattle.platform.configitem.context.data.ServiceDnsEntryData;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.jooq.Condition;

public class DnsInfoDaoImpl extends AbstractJooqDao implements DnsInfoDao {

    @Inject
    NetworkDao ntwkDao;

    @Inject
    ObjectManager objManager;

    @Override
    public List<DnsEntryData> getInstanceLinksDnsData(final Instance instance) {
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
                data.setSourceIpAddress(((IpAddress) input.get(2)).getAddress());
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
        MultiRecordMapper<ServiceDnsEntryData> mapper = new MultiRecordMapper<ServiceDnsEntryData>() {
            @Override
            protected ServiceDnsEntryData map(List<Object> input) {
                Service clientService = (Service) input.get(0);
                Service targetService = (Service) input.get(1);
                ServiceConsumeMap consumeMap = (ServiceConsumeMap) input.get(2);
                ServiceDnsEntryData data = new ServiceDnsEntryData(clientService, targetService, consumeMap);
                return data;
            }
        };

        ServiceTable clientService = mapper.add(SERVICE);
        ServiceTable targetService = mapper.add(SERVICE);
        ServiceConsumeMapTable serviceConsumeMap = mapper.add(SERVICE_CONSUME_MAP);

        Condition condition = null;
        Condition commonCondition = serviceConsumeMap.ID.isNotNull().and(serviceConsumeMap.REMOVED.isNull())
                .and(serviceConsumeMap.STATE.in(CommonStatesConstants.ACTIVATING,
                        CommonStatesConstants.ACTIVE));
        if (links) {
            condition = commonCondition.and(clientService.KIND.ne(ServiceDiscoveryConstants.KIND.DNSSERVICE.name()));
        } else {
            condition = (clientService.KIND.ne(ServiceDiscoveryConstants.KIND.DNSSERVICE.name())
                    .and(targetService.ENVIRONMENT_ID.eq(clientService.ENVIRONMENT_ID))
                    .and(serviceConsumeMap.ID.isNull())).or(clientService.KIND.eq(
                    ServiceDiscoveryConstants.KIND.DNSSERVICE.name()).and(commonCondition));
        }

        List<ServiceDnsEntryData> serviceDnsEntries = create()
                .select(mapper.fields())
                .from(clientService)
                .join(targetService)
                .on(targetService.ACCOUNT_ID.eq(clientService.ACCOUNT_ID))
                .leftOuterJoin(serviceConsumeMap)
                .on(serviceConsumeMap.SERVICE_ID.eq(clientService.ID).and(
                        serviceConsumeMap.CONSUMED_SERVICE_ID.eq(targetService.ID))
                        .and(serviceConsumeMap.REMOVED.isNull()))
                .where(targetService.REMOVED.isNull())
                        .and(clientService.REMOVED.isNull())
                .and(condition)
                .fetch().map(mapper);

        Nic nic = ntwkDao.getPrimaryNic(instance.getId());
        long vnetId = nic.getVnetId();
        return convertToDnsEntryData(isVIPProvider, serviceDnsEntries, instance.getAccountId(), vnetId);
    }

    protected List<DnsEntryData> convertToDnsEntryData(final boolean isVIPProvider, List<ServiceDnsEntryData> serviceDnsData,
            long accountId, long vnetId) {
        final Map<Long, IpAddress> instanceIdToHostIpMap = getInstanceWithHostNetworkingToIpMap(accountId);
        Map<Long, List<ServiceInstanceData>> servicesClientInstances = getServiceInstancesData(accountId,
                true, vnetId);
        Map<Long, List<ServiceInstanceData>> servicesTargetInstances = getServiceInstancesData(accountId,
                false, vnetId);
        Map<Long, List<ServiceDnsEntryData>> clientServiceIdToServiceData = new HashMap<>();
        for (ServiceDnsEntryData data : serviceDnsData) {
            Long clientServiceId = data.getClientService().getId();
            List<ServiceDnsEntryData> existingData = clientServiceIdToServiceData.get(clientServiceId);
            if (existingData == null) {
                existingData = new ArrayList<>();
            }
            existingData.add(data);
            clientServiceIdToServiceData.put(clientServiceId, existingData);
        }
 
        List<DnsEntryData> returnData = new ArrayList<>();

        for (ServiceDnsEntryData serviceData : serviceDnsData) {
            DnsEntryData data = new DnsEntryData();
            Service clientService = serviceData.getClientService();
            Map<String, Map<String, String>> resolve = new HashMap<>();
            Map<String, String> resolveCname = new HashMap<>();
            Service targetService = serviceData.getTargetService();
            List<ServiceInstanceData> targetInstancesData = populateTargetInstancesData(servicesTargetInstances,
                    clientServiceIdToServiceData, targetService);
            
            for (ServiceInstanceData targetInstance : targetInstancesData) {
                populateResolveInfo(isVIPProvider, instanceIdToHostIpMap, serviceData, clientService, resolve,
                        resolveCname, targetService, targetInstance);
            }

            if (servicesClientInstances.containsKey(clientService.getId())) {
                for (ServiceInstanceData clientInstance : servicesClientInstances.get(clientService.getId())) {
                    String clientIp = getIpAddress(clientInstance, true, instanceIdToHostIpMap);
                    data.setSourceIpAddress(clientIp);
                    data.setResolveServicesAndContainers(resolve);
                    data.setInstance(clientInstance.getInstance());
                    data.setResolveCname(resolveCname);
                    returnData.add(data);
                }
            }
        }
        return returnData;
    }

    protected List<ServiceInstanceData> populateTargetInstancesData(
            Map<Long, List<ServiceInstanceData>> servicesTargetInstances,
            Map<Long, List<ServiceDnsEntryData>> clientServiceIdToServiceData, Service targetService) {
        List<ServiceInstanceData> targetInstancesData = new ArrayList<>();
        if (targetService.getKind().equalsIgnoreCase(ServiceDiscoveryConstants.KIND.DNSSERVICE.name())) {
            List<ServiceDnsEntryData> behindAlias = clientServiceIdToServiceData.get(targetService.getId());
            if (behindAlias != null) {
                for (ServiceDnsEntryData behindAliasEntry : behindAlias) {
                    List<ServiceInstanceData> toAdd = servicesTargetInstances.get(behindAliasEntry
                            .getTargetService()
                            .getId());
                    if (toAdd != null) {
                        targetInstancesData.addAll(toAdd);
                    }
                }
            }
        } else {
            List<ServiceInstanceData> toAdd = servicesTargetInstances.get(targetService
                    .getId());
            if (toAdd != null) {
                targetInstancesData.addAll(toAdd);
            }
        }
        return targetInstancesData;
    }

    protected void populateResolveInfo(final boolean isVIPProvider, final Map<Long, IpAddress> instanceIdToHostIpMap,
            ServiceDnsEntryData serviceData, Service clientService, Map<String, Map<String, String>> resolve,
            Map<String, String> resolveCname, Service targetService, ServiceInstanceData targetInstance) {
        String targetInstanceName = targetInstance.getInstance() == null ? null : targetInstance
                .getInstance().getName();

        String dnsName = getDnsName(targetService, serviceData.getConsumeMap(),
                targetInstance.getExposeMap(), false);


        String targetIp = isVIPProvider ? clientService.getVip() : getIpAddress(
                targetInstance, false,
                instanceIdToHostIpMap);
        if (targetIp != null) {
            Map<String, String> ips = resolve.get(dnsName);
            if (ips == null) {
                ips = new HashMap<>();
            }
            ips.put(targetIp, targetInstanceName);

            resolve.put(dnsName, ips);
        } else {
            String cname = targetInstance.getExposeMap().getHostName();
            if (cname != null) {
                resolveCname.put(dnsName, cname);
            }
        }
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

    protected String getIpAddress(ServiceInstanceData serviceInstanceData, boolean isSource, Map<Long, IpAddress> instanceIdToHostIpMap) {
        Nic nic = serviceInstanceData.getNic();
        ServiceExposeMap exposeMap = serviceInstanceData.getExposeMap();
        IpAddress ipAddr = serviceInstanceData.getIpAddress();
        String ip = null;

        if (isSource
                && serviceInstanceData.getService().getKind()
                        .equalsIgnoreCase(ServiceDiscoveryConstants.KIND.SERVICE.name())) {
            if (ipAddr != null) {
                ip = ipAddr.getAddress();
            }
        } else {
            if (ipAddr != null) {
                ip = ipAddr.getAddress();
            } else {
                ip = exposeMap.getIpAddress();
            }
        }

        if (nic == null || nic.getDeviceNumber().equals(0)) {
            return ip;
        } else {
            IpAddress hostIp = instanceIdToHostIpMap.get(nic.getInstanceId());
            if (hostIp != null) {
                if (isSource) {
                    return "default";
                }
                return hostIp.getAddress();
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

    protected Map<Long, List<ServiceInstanceData>> getServiceInstancesData(long accountId, boolean client, long vnetId) {
        Map<Long, List<ServiceInstanceData>> returnData = new HashMap<>();
        List<ServiceInstanceData> serviceData = new ArrayList<>();
        if (client) {
            serviceData = getServiceInstanceInstancesData(accountId, true, vnetId);
        } else {
            serviceData = getServiceInstanceInstancesData(accountId, false, vnetId);
        }

        for (ServiceInstanceData data : serviceData) {
            List<ServiceInstanceData> existingData = returnData.get(data.getService().getId());
            if (existingData == null) {
                existingData = new ArrayList<>();
            }
            existingData.add(data);
            returnData.put(data.getService().getId(), existingData);
        }
        return returnData;
    }

    protected List<ServiceInstanceData> getServiceInstanceInstancesData(long accountId, boolean client, long vnetId) {
        MultiRecordMapper<ServiceInstanceData> mapper = new MultiRecordMapper<ServiceInstanceData>() {
            @Override
            protected ServiceInstanceData map(List<Object> input) {
                Service service = (Service) input.get(0);
                IpAddress ip = (IpAddress) input.get(1);
                Instance instance = (Instance) input.get(2);
                ServiceExposeMap exposeMap = (ServiceExposeMap) input.get(3);
                Nic nic = (Nic) input.get(4);
                ServiceInstanceData data = new ServiceInstanceData(service, ip, instance, exposeMap, nic);
                return data;
            }
        };
        

        ServiceTable service = mapper.add(SERVICE);
        IpAddressTable ipAddress = mapper.add(IP_ADDRESS);
        InstanceTable instance = mapper.add(INSTANCE);
        ServiceExposeMapTable exposeMap = mapper.add(SERVICE_EXPOSE_MAP);
        NicTable nic = mapper.add(NIC);
        
        Condition condition = null;
        if (client) {
            condition = ipAddress.ROLE.eq(IpAddressConstants.ROLE_PRIMARY).and(nic.VNET_ID.eq(vnetId));
        } else {
            condition = (ipAddress.ROLE.isNull().and(ipAddress.ADDRESS.isNull())).or(ipAddress.ROLE
                    .eq(IpAddressConstants.ROLE_PRIMARY));
        }

        return create()
                .select(mapper.fields())
                .from(service)
                .join(exposeMap)
                .on(service.ID.eq(exposeMap.SERVICE_ID))
                .leftOuterJoin(instance)
                .on(instance.ID.eq(exposeMap.INSTANCE_ID))
                .leftOuterJoin(nic)
                .on(nic.INSTANCE_ID.eq(exposeMap.INSTANCE_ID))
                .leftOuterJoin(IP_ADDRESS_NIC_MAP)
                .on(IP_ADDRESS_NIC_MAP.NIC_ID.eq(nic.ID))
                .leftOuterJoin(ipAddress)
                .on(IP_ADDRESS_NIC_MAP.IP_ADDRESS_ID.eq(ipAddress.ID))
                .where(service.ACCOUNT_ID.eq(accountId))
                .and(service.REMOVED.isNull())
                .and(exposeMap.REMOVED.isNull())
                .and(nic.REMOVED.isNull())
                .and(ipAddress.REMOVED.isNull())
                .and(instance.REMOVED.isNull())
                .and(instance.STATE.isNull().or(instance.STATE.in(InstanceConstants.STATE_RUNNING,
                        InstanceConstants.STATE_STARTING)))
                .and(instance.HEALTH_STATE.isNull().or(
                        instance.HEALTH_STATE.eq(HealthcheckConstants.HEALTH_STATE_HEALTHY)))
                .and(condition)
                .fetch().map(mapper);
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
