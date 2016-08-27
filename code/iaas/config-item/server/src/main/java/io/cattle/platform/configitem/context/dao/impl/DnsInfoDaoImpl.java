package io.cattle.platform.configitem.context.dao.impl;

import static io.cattle.platform.core.model.tables.StackTable.STACK;
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
import io.cattle.platform.core.model.Stack;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.InstanceHostMap;
import io.cattle.platform.core.model.InstanceLink;
import io.cattle.platform.core.model.IpAddress;
import io.cattle.platform.core.model.Network;
import io.cattle.platform.core.model.Nic;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.ServiceConsumeMap;
import io.cattle.platform.core.model.ServiceExposeMap;
import io.cattle.platform.core.model.tables.StackTable;
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
import io.cattle.platform.object.util.DataUtils;
import io.cattle.platform.servicediscovery.api.constants.ServiceDiscoveryConstants;
import io.cattle.platform.servicediscovery.api.util.ServiceDiscoveryDnsUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.jooq.Condition;

public class DnsInfoDaoImpl extends AbstractJooqDao implements DnsInfoDao {

    @Inject
    NetworkDao ntwkDao;

    @Inject
    ObjectManager objManager;

    @Override
    public List<DnsEntryData> getInstanceLinksDnsData(final Instance instance) {
        // adds all instance links
        MultiRecordMapper<DnsEntryData> mapper = new MultiRecordMapper<DnsEntryData>() {
            @Override
            protected DnsEntryData map(List<Object> input) {
                Map<String, Map<String, String>> resolve = new HashMap<>();
                Map<String, String> ips = new HashMap<>();
                String targetInstanceName = input.get(4) == null ? null : ((Instance) input.get(4)).getName() + "."
                        + ServiceDiscoveryDnsUtil.RANCHER_NAMESPACE + ".";
                ips.put(((IpAddress) input.get(1)).getAddress(), targetInstanceName);
                resolve.put(((InstanceLink) input.get(0)).getLinkName().toLowerCase() + "."
                        + ServiceDiscoveryDnsUtil.RANCHER_NAMESPACE + ".", ips);
                String sourceIp = ((IpAddress) input.get(2)).getAddress();
                Instance instance = (Instance)input.get(3);
                List<String> dnsSearch = new ArrayList<>();
                dnsSearch.add(ServiceDiscoveryDnsUtil.RANCHER_NAMESPACE);
                DnsEntryData data = new DnsEntryData(sourceIp, resolve, null, instance,
                        dnsSearch);
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
                        .and(targetInstance.STATE.in(InstanceConstants.STATE_RUNNING))
                        .and(targetInstance.HEALTH_STATE.isNull().or(
                                targetInstance.HEALTH_STATE.eq(HealthcheckConstants.HEALTH_STATE_HEALTHY))))
                .fetch().map(mapper);
    }

    @Override
    public List<DnsEntryData> getServiceDnsData(final Instance instance, boolean forDefault) {
        MultiRecordMapper<ServiceDnsEntryData> mapper = new MultiRecordMapper<ServiceDnsEntryData>() {
            @Override
            protected ServiceDnsEntryData map(List<Object> input) {
                Service clientService = (Service) input.get(0);
                Service targetService = (Service) input.get(1);
                ServiceConsumeMap consumeMap = (ServiceConsumeMap) input.get(2);
                Stack clientStack = (Stack) input.get(3);
                Stack targetStack = (Stack) input.get(4);
                ServiceDnsEntryData data = new ServiceDnsEntryData(clientService, targetService, consumeMap,
                        clientStack, targetStack);
                return data;
            }
        };

        ServiceTable clientService = mapper.add(SERVICE);
        ServiceTable targetService = mapper.add(SERVICE);
        ServiceConsumeMapTable serviceConsumeMap = mapper.add(SERVICE_CONSUME_MAP);
        StackTable clientStack = mapper.add(STACK);
        StackTable targetStack = mapper.add(STACK);

        Condition condition = null;
        if (forDefault) {
            // all services records
            condition = (clientService.KIND.ne(ServiceDiscoveryConstants.KIND_DNS_SERVICE)
                    .and(targetService.ID.eq(clientService.ID))
                    .and(serviceConsumeMap.ID.isNull()))
                    .or((clientService.KIND.eq(ServiceDiscoveryConstants.KIND_DNS_SERVICE)
                            .and(serviceConsumeMap.ID.isNotNull()
                    .and(serviceConsumeMap.REMOVED.isNull())
                    .and(serviceConsumeMap.STATE.in(CommonStatesConstants.ACTIVATING,
                            CommonStatesConstants.ACTIVE)))));
        } else {
            // all linked records
            condition = serviceConsumeMap.ID.isNotNull().and(serviceConsumeMap.REMOVED.isNull())
                                    .and(serviceConsumeMap.STATE.in(CommonStatesConstants.ACTIVATING,
                            CommonStatesConstants.ACTIVE));
        }

        List<ServiceDnsEntryData> serviceDnsEntries = create()
                .select(mapper.fields())
                .from(clientService)
                .join(targetService)
                .on(targetService.ACCOUNT_ID.eq(clientService.ACCOUNT_ID))
                .join(clientStack)
                .on(clientService.STACK_ID.eq(clientStack.ID))
                .join(targetStack)
                .on(targetService.STACK_ID.eq(targetStack.ID))
                .leftOuterJoin(serviceConsumeMap)
                .on(serviceConsumeMap.SERVICE_ID.eq(clientService.ID).and(
                        serviceConsumeMap.CONSUMED_SERVICE_ID.eq(targetService.ID))
                        .and(serviceConsumeMap.REMOVED.isNull()))
                .where(targetService.REMOVED.isNull())
                        .and(clientService.REMOVED.isNull())
                .and(condition)
                .fetch().map(mapper);

        Nic nic = ntwkDao.getPrimaryNic(instance.getId());
        if (nic == null) {
            return new ArrayList<>();
        }
        long vnetId = nic.getVnetId();
        return convertToDnsEntryData(serviceDnsEntries, instance.getAccountId(), vnetId, forDefault);
    }

    protected List<DnsEntryData> convertToDnsEntryData(List<ServiceDnsEntryData> serviceDnsData,
            long accountId, long vnetId, boolean forDefault) {
        final Map<Long, IpAddress> instanceIdToHostIpMap = getInstanceWithHostNetworkingToIpMap(accountId);
        Map<Long, List<ServiceInstanceData>> servicesClientInstances = getServiceIdToServiceInstancesData(accountId,
                true, vnetId);
        Map<Long, List<ServiceInstanceData>> servicesTargetInstances = getServiceIdToServiceInstancesData(accountId,
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
            Service clientService = serviceData.getClientService();
            Map<String, Map<String, String>> resolve = new HashMap<>();
            Map<String, String> resolveCname = new HashMap<>();
            Service targetService = serviceData.getTargetService();
            List<ServiceInstanceData> targetInstancesData = populateTargetInstancesData(servicesTargetInstances,
                    clientServiceIdToServiceData, targetService);
            String aliasName = null;
            boolean isAliasService = false;
            if (serviceData.getConsumeMap() != null) {
                if (clientService.getKind().equalsIgnoreCase(ServiceDiscoveryConstants.KIND_DNS_SERVICE)) {
                    aliasName = ServiceDiscoveryDnsUtil.getFqdn(serviceData.getClientStack(),
                            serviceData.getClientService(), serviceData.getClientService().getName());
                    isAliasService = true;
                } else {
                    if (!StringUtils.isEmpty(serviceData.getConsumeMap().getName())) {
                        aliasName = serviceData.getConsumeMap().getName();
                    } else {
                        aliasName = targetService.getName();
                    }
                }
            }

            boolean isLink = serviceData.getConsumeMap() != null;
            boolean self = clientService.getId().equals(targetService.getId()) && !forDefault;
            for (ServiceInstanceData targetInstance : targetInstancesData) {
                String dnsPrefix = targetInstance.getExposeMap() != null ? targetInstance.getExposeMap()
                        .getDnsPrefix()
                        : null;
                if (dnsPrefix != null && isLink) {
                    // skip sidekick from link resolution
                    continue;
                }
                populateResolveInfo(targetInstance, self, aliasName, dnsPrefix, instanceIdToHostIpMap,
                        resolveCname, resolve, isAliasService, serviceData.getClientStack(), serviceData.getClientService());
            }

            List<ServiceInstanceData> clientInstanceData = servicesClientInstances.get(clientService.getId());

            if (forDefault) {
                DnsEntryData data = new DnsEntryData("default", resolve, resolveCname, null, null);
                returnData.add(data);
            }

            if (clientInstanceData != null) {
                    for (ServiceInstanceData clientInstance : clientInstanceData) {
                        String clientIp = getIpAddress(clientInstance, instanceIdToHostIpMap, true);
                        if (!forDefault) {
                            DnsEntryData data = new DnsEntryData(clientIp, resolve, resolveCname,
                                    clientInstance.getInstance(), null);
                            returnData.add(data);
                        }
                        // to add search domains
                        List<String> searchDomains = ServiceDiscoveryDnsUtil.getNamespaces(
                                serviceData.getClientStack(),
                                serviceData.getClientService(), clientInstance.getExposeMap().getDnsPrefix());
                        DnsEntryData data = new DnsEntryData(clientIp, null, null,
                            clientInstance.getInstance(), searchDomains);
                        returnData.add(data);
                    }
            }
        }

        if (forDefault) {
            Map<String, String> metadataIp = new HashMap<>();
            metadataIp.put(ServiceDiscoveryDnsUtil.NETWORK_AGENT_IP, null);
            Map<String, Map<String, String>> resolve = new HashMap<>();
            resolve.put(ServiceDiscoveryDnsUtil.METADATA_FQDN, metadataIp);
            DnsEntryData data = new DnsEntryData("default", resolve, null, null, null);
            returnData.add(data);
        }

        return returnData;
    }

    protected List<ServiceInstanceData> populateTargetInstancesData(
            Map<Long, List<ServiceInstanceData>> servicesTargetInstances,
            Map<Long, List<ServiceDnsEntryData>> clientServiceIdToServiceData, Service targetService) {
        List<ServiceInstanceData> targetInstancesData = new ArrayList<>();
        if (targetService.getKind().equalsIgnoreCase(ServiceDiscoveryConstants.KIND_DNS_SERVICE)) {
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

    protected void populateResolveInfo(ServiceInstanceData targetInstance, boolean self,
            String aliasName, String dnsPrefix, final Map<Long, IpAddress> instanceIdToHostIpMap,
            Map<String, String> resolveCname, Map<String, Map<String, String>> resolve, boolean isAliasService,
            Stack clientStack, Service clientService) {
        String targetInstanceName = targetInstance.getInstance() == null ? null : targetInstance
                .getInstance().getName();

        List<String> fqdns = new ArrayList<>();
        if (!StringUtils.isEmpty(aliasName) && !isAliasService) {
            fqdns.add(aliasName.toLowerCase() + "."
                    + ServiceDiscoveryDnsUtil.getStackNamespace(clientStack, clientService) + ".");
            fqdns.add(aliasName.toLowerCase() + "." + ServiceDiscoveryDnsUtil.getGlobalNamespace(clientService) + ".");
        } else {
            fqdns.add(ServiceDiscoveryDnsUtil.getDnsName(targetInstance.getService(), targetInstance.getStack(),
                    aliasName, dnsPrefix, self));
        }
        for (String fqdn : fqdns) {
            addToResolve(targetInstance, aliasName, instanceIdToHostIpMap, resolveCname, resolve, targetInstanceName,
                    fqdn);
        }
    }

    protected void addToResolve(ServiceInstanceData targetInstance, String aliasName,
            final Map<Long, IpAddress> instanceIdToHostIpMap, Map<String, String> resolveCname,
            Map<String, Map<String, String>> resolve, String targetInstanceName, String fqdn) {
        String targetIp = getIpAddress(targetInstance, instanceIdToHostIpMap, false);
        if (!StringUtils.isEmpty(targetIp)) {
            Map<String, String> ipToInstanceName = resolve.get(fqdn);
            if (ipToInstanceName == null) {
                ipToInstanceName = new HashMap<>();
            }
            if (getVip(targetInstance.getService()) != null) {
                targetIp = targetInstance.getService().getVip();
            }
            if (aliasName == null) {
                if (StringUtils.isEmpty(targetInstanceName)) {
                    ipToInstanceName.put(targetIp, null);
                } else {
                    String dnsName;
                    if (targetInstance.getService().getKind().equalsIgnoreCase("kubernetesservice")) {
                        dnsName = ServiceDiscoveryDnsUtil.getServiceNamespace(targetInstance.getStack(), targetInstance.getService());
                    } else {
                        dnsName = ServiceDiscoveryDnsUtil.getGlobalNamespace(targetInstance.getService());
                    }
                    ipToInstanceName.put(
                            targetIp,
                            targetInstanceName
                            + "." + dnsName
                            + ".");
                }
            } else {
                ipToInstanceName.put(targetIp, null);
            }
            resolve.put(fqdn, ipToInstanceName);
        } else {
            String cname = targetInstance.getExposeMap().getHostName();
            if (cname != null) {
                resolveCname.put(fqdn, cname + ".");
            }
        }
    }

    protected String getVip(Service service) {
        String vip = service.getVip();
        //indicator that its pre-upgraded setup that had vip set for every service by default
        // vip will be set only
        // a) field_set_vip is set via API
        // b) for k8s services
        Map<String, Object> data = new HashMap<>();
        data.putAll(DataUtils.getFields(service));
        Object vipObj = data.get(ServiceDiscoveryConstants.FIELD_SET_VIP);
        boolean setVip = vipObj != null && Boolean.valueOf(vipObj.toString());
        if (setVip
                || service.getKind().equalsIgnoreCase("kubernetesservice")) {
            return vip;
        }
        return null;
    }

    protected String getIpAddress(ServiceInstanceData serviceInstanceData, Map<Long, IpAddress> instanceIdToHostIpMap, boolean isSource) {
        Nic nic = serviceInstanceData.getNic();
        ServiceExposeMap exposeMap = serviceInstanceData.getExposeMap();
        IpAddress ipAddr = serviceInstanceData.getIpAddress();
        String ip = null;

        if (isSource
                && serviceInstanceData.getService().getKind()
                        .equalsIgnoreCase(ServiceDiscoveryConstants.KIND_SERVICE)) {
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

    @Override
    public Map<Long, IpAddress> getInstanceWithHostNetworkingToIpMap(long accountId) {
        List<HostInstanceIpData> data = getHostContainerIpData(accountId);
        Map<Long, IpAddress> instanceIdToHostIpMap = new HashMap<>();
        for (HostInstanceIpData entry : data) {
            instanceIdToHostIpMap.put(entry.getInstanceHostMap().getInstanceId(), entry.getIpAddress());
        }

        return instanceIdToHostIpMap;
    }

    protected Map<Long, List<ServiceInstanceData>> getServiceIdToServiceInstancesData(long accountId, boolean client, long vnetId) {
        Map<Long, List<ServiceInstanceData>> returnData = new HashMap<>();
        List<ServiceInstanceData> serviceData = getServiceInstancesData(accountId, client, vnetId);

        for (ServiceInstanceData data : serviceData) {
            List<ServiceInstanceData> existingData = returnData.get(data.getService().getId());
            if (existingData == null) {
                existingData = new ArrayList<>();
            }
            existingData.add(data);
            returnData.put(data.getService().getId(), existingData);
        }
        
        // parse the health check
        Map<Long, List<ServiceInstanceData>> returnDataFiltered = new HashMap<>();
        if (client) {
            returnDataFiltered.putAll(returnData);
        } else {
            for (Long serviceId : returnData.keySet()) {
                List<ServiceInstanceData> originalInstances = returnData.get(serviceId);
                List<ServiceInstanceData> filteredInstances = new ArrayList<>();
                for (int i = 0; i < originalInstances.size(); i++) {
                    ServiceInstanceData instance = originalInstances.get(i);
                    if (instance.getInstance() != null) {
                        boolean isHealthy = instance.getInstance().getHealthState() == null
                                || instance.getInstance().getHealthState()
                                        .equalsIgnoreCase(HealthcheckConstants.HEALTH_STATE_HEALTHY);
                        boolean isRunning = Arrays.asList(InstanceConstants.STATE_RUNNING).contains(instance.getInstance().getState());
                        if (isRunning && isHealthy) {
                            filteredInstances.add(instance);
                        } else if (i == originalInstances.size() - 1 && filteredInstances.size() == 0) {
                            filteredInstances.add(instance);
                        }
                    } else {
                        filteredInstances.add(instance);
                    }
                }
                returnDataFiltered.put(serviceId, filteredInstances);
            }
        }

        return returnDataFiltered;
    }

    protected List<ServiceInstanceData> getServiceInstancesData(long accountId, boolean client, long vnetId) {
        List<ServiceInstanceData> data = getServiceInstancesDataImpl(accountId, client, vnetId);
        Map<Long, IpAddress> instanceIdToIp = new HashMap<>();
        for (ServiceInstanceData d : data) {
            if (d.getInstance() != null) {
                instanceIdToIp.put(d.getInstance().getId(), d.getIpAddress());
            }
        }

        for (ServiceInstanceData d : data) {
            if (d.getInstance() != null && d.getInstance().getNetworkContainerId() != null) {
                        d.setIpAddress(instanceIdToIp.get(d.getInstance().getNetworkContainerId()));
            }
        }
        return data;
    }

    protected List<ServiceInstanceData> getServiceInstancesDataImpl(long accountId, boolean client, long vnetId) {
        MultiRecordMapper<ServiceInstanceData> mapper = new MultiRecordMapper<ServiceInstanceData>() {
            @Override
            protected ServiceInstanceData map(List<Object> input) {
                Service service = (Service) input.get(0);
                IpAddress ip = (IpAddress) input.get(1);
                Instance instance = (Instance) input.get(2);
                ServiceExposeMap exposeMap = (ServiceExposeMap) input.get(3);
                Nic nic = (Nic) input.get(4);
                Stack stack = (Stack) input.get(5);
                ServiceInstanceData data = new ServiceInstanceData(stack, service, ip, instance, exposeMap, nic);
                return data;
            }
        };
        

        ServiceTable service = mapper.add(SERVICE, SERVICE.ID, SERVICE.VIP, SERVICE.KIND, SERVICE.NAME);
        IpAddressTable ipAddress = mapper.add(IP_ADDRESS, IP_ADDRESS.ADDRESS);
        InstanceTable instance = mapper.add(INSTANCE, INSTANCE.NAME, INSTANCE.HEALTH_STATE, INSTANCE.DNS_INTERNAL,
                INSTANCE.DNS_SEARCH_INTERNAL,
                INSTANCE.STATE,
                INSTANCE.NETWORK_CONTAINER_ID);
        ServiceExposeMapTable exposeMap = mapper.add(SERVICE_EXPOSE_MAP);
        NicTable nic = mapper.add(NIC, NIC.DEVICE_NUMBER, NIC.INSTANCE_ID);
        StackTable stack = mapper.add(STACK, STACK.NAME);
        Condition condition = null;
        if (client) {
            condition = ipAddress.ROLE.eq(IpAddressConstants.ROLE_PRIMARY).and(nic.VNET_ID.eq(vnetId));
        } else {
            condition = (ipAddress.ROLE.isNull().and(ipAddress.ADDRESS.isNull().or(ipAddress.ADDRESS.eq(""))))
                    .or(ipAddress.ROLE
                    .eq(IpAddressConstants.ROLE_PRIMARY));
        }

        return create()
                .select(mapper.fields())
                .from(service)
                .join(exposeMap)
                .on(service.ID.eq(exposeMap.SERVICE_ID))
                .join(stack)
                .on(stack.ID.eq(service.STACK_ID))
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
