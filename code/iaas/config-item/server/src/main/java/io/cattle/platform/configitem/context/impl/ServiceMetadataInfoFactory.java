package io.cattle.platform.configitem.context.impl;

import static io.cattle.platform.core.model.tables.AccountLinkTable.*;
import static io.cattle.platform.core.model.tables.AccountTable.*;
import static io.cattle.platform.core.model.tables.InstanceHostMapTable.*;
import static io.cattle.platform.core.model.tables.ServiceConsumeMapTable.*;
import static io.cattle.platform.core.model.tables.ServiceTable.*;
import static io.cattle.platform.core.model.tables.StackTable.*;
import io.cattle.platform.configitem.context.dao.MetaDataInfoDao;
import io.cattle.platform.configitem.context.dao.MetaDataInfoDao.Version;
import io.cattle.platform.configitem.context.data.metadata.common.ContainerMetaData;
import io.cattle.platform.configitem.context.data.metadata.common.DefaultMetaData;
import io.cattle.platform.configitem.context.data.metadata.common.HostMetaData;
import io.cattle.platform.configitem.context.data.metadata.common.MetaHelperInfo;
import io.cattle.platform.configitem.context.data.metadata.common.NetworkMetaData;
import io.cattle.platform.configitem.context.data.metadata.common.SelfMetaData;
import io.cattle.platform.configitem.context.data.metadata.common.ServiceMetaData;
import io.cattle.platform.configitem.context.data.metadata.common.StackMetaData;
import io.cattle.platform.configitem.context.data.metadata.version1.ServiceMetaDataVersion1;
import io.cattle.platform.configitem.context.data.metadata.version1.StackMetaDataVersion1;
import io.cattle.platform.configitem.context.data.metadata.version2.ContainerMetaDataVersion3;
import io.cattle.platform.configitem.context.data.metadata.version2.ServiceMetaDataVersion2;
import io.cattle.platform.configitem.context.data.metadata.version2.ServiceMetaDataVersion3;
import io.cattle.platform.configitem.context.data.metadata.version2.StackMetaDataVersion2;
import io.cattle.platform.configitem.context.data.metadata.version2.StackMetaDataVersion3;
import io.cattle.platform.configitem.server.model.ConfigItem;
import io.cattle.platform.configitem.server.model.impl.ArchiveContext;
import io.cattle.platform.core.addon.InstanceHealthCheck;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.dao.GenericMapDao;
import io.cattle.platform.core.dao.LoadBalancerInfoDao;
import io.cattle.platform.core.dao.NetworkDao;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.AccountLink;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.InstanceHostMap;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.ServiceConsumeMap;
import io.cattle.platform.core.model.Stack;
import io.cattle.platform.core.util.LBMetadataUtil.LBConfigMetadataStyle;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.servicediscovery.api.dao.ServiceConsumeMapDao;
import io.cattle.platform.servicediscovery.api.util.ServiceDiscoveryUtil;
import io.cattle.platform.util.exception.ExceptionUtils;
import io.github.ibuildthecloud.gdapi.condition.Condition;
import io.github.ibuildthecloud.gdapi.condition.ConditionType;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;

@Named
public class ServiceMetadataInfoFactory extends AbstractAgentBaseContextFactory {

    private static final ThreadLocal<Yaml> TL = new ThreadLocal<Yaml>(){
        @Override
        protected Yaml initialValue() {
            return getYaml();
        }
    };

    @Inject
    ServiceConsumeMapDao consumeMapDao;

    @Inject
    MetaDataInfoDao metaDataInfoDao;

    @Inject
    GenericMapDao mapDao;

    @Inject
    JsonMapper jsonMapper;

    @Inject
    LoadBalancerInfoDao lbInfoDao;

    @Inject
    NetworkDao networkDao;

    @Override
    protected void populateContext(Agent agent, Instance instance, ConfigItem item, ArchiveContext context) {
        Map<String, Object> dataWithVersionTag = generateData(instance, context.getVersion());
        context.getData().put("data", generateYml(dataWithVersionTag));
    }

    public void writeMetadata(Instance instance, String itemVersion, OutputStream os) {
        Map<String, Object> dataWithVersionTag = generateData(instance, itemVersion);
        try {
            Yaml yaml = TL.get();
            yaml.dump(dataWithVersionTag, new OutputStreamWriter(os));
        } catch (Throwable t) {
            TL.remove();
            ExceptionUtils.rethrowExpectedRuntime(t);
        }
    }

    protected Map<String, Object> generateData(Instance instance, String itemVersion) {
        Map<String, Object> dataWithVersionTag = new HashMap<>();

        if (instance == null) {
            return dataWithVersionTag;
        }

        InstanceHostMap hostMap = objectManager.findAny(InstanceHostMap.class, INSTANCE_HOST_MAP.INSTANCE_ID,
                instance.getId());
        if (hostMap == null) {
            return dataWithVersionTag;
        }

        MetaHelperInfo helperInfo = fetchHelperData(objectManager.loadResource(Account.class, instance.getAccountId()));

        List<ContainerMetaData> containersMD = fetchContainersMetadata(helperInfo);

        Map<String, StackMetaData> stackNameToStack = new HashMap<>();
        Map<Long, Map<String, ServiceMetaData>> serviceIdToServiceLaunchConfigs = new HashMap<>();

        List<? extends Service> allSvcs = getAllServices(helperInfo);

        Map<Long, Service> svcIdsToSvc = getServiceIdToService(allSvcs);
        Map<Long, List<ServiceConsumeMap>> svcIdToSvcLinks = getServiceIdToServiceLinks(helperInfo);
        populateStacksServicesInfo(helperInfo, stackNameToStack, serviceIdToServiceLaunchConfigs, allSvcs);

        long agentHostId = hostMap.getHostId();
        Map<String, Object> versionToData = new HashMap<>();
        for (MetaDataInfoDao.Version version : MetaDataInfoDao.Version.values()) {
            Object data = versionToData.get(version.getValue());
            if (data == null) {
                data = getFullMetaData(itemVersion, containersMD, stackNameToStack, serviceIdToServiceLaunchConfigs, version,
                        svcIdsToSvc, svcIdToSvcLinks, agentHostId, helperInfo);
                versionToData.put(version.getValue(), data);
            }
            dataWithVersionTag.put(version.getTag(), data);
        }

        return dataWithVersionTag;
    }

    protected List<Service> getAllServices(MetaHelperInfo helperInfo) {
        List<Service> services = objectManager.find(Service.class, SERVICE.ACCOUNT_ID,
                helperInfo.getAccount().getId(), SERVICE.REMOVED, null);
        if (!helperInfo.getOtherAccountsServicesIds().isEmpty()) {
            List<Object> servicesIds = new ArrayList<>();
            servicesIds.addAll(helperInfo.getOtherAccountsServicesIds());
            services.addAll(objectManager.find(Service.class, SERVICE.ID,
                    new Condition(ConditionType.IN, servicesIds), SERVICE.REMOVED, null));
        }
        return services;
    }

    private MetaHelperInfo fetchHelperData(Account account) {
        Map<Long, Account> accounts = new HashMap<>();
        Set<Long> linkedServicesIds = new HashSet<>();
        Set<Long> linkedStackIds = new HashSet<>();
        List<? extends Account> allAccounts = objectManager.find(Account.class, ACCOUNT.REMOVED, new Condition(
                ConditionType.NULL));
        Map<Long, Account> allAccountsMap = new HashMap<>();
        for (Account a : allAccounts) {
            allAccountsMap.put(a.getId(), a);
        }
        // fetch accounts/services that are linked TO your account
        accounts.put(account.getId(), account);
        List<? extends AccountLink> accountLinks = objectManager.find(AccountLink.class, ACCOUNT_LINK.ACCOUNT_ID,
                account.getId(), ACCOUNT_LINK.REMOVED, null);
        for (AccountLink accountLink : accountLinks) {
            Long accountId = accountLink.getLinkedAccountId();
            accounts.put(accountId, allAccountsMap.get(accountId));
        }
        
        // fetch accounts/services that your account is linked TO
        accountLinks = objectManager.find(AccountLink.class, ACCOUNT_LINK.LINKED_ACCOUNT_ID,
                account.getId(), ACCOUNT_LINK.REMOVED, null);
        for (AccountLink accountLink : accountLinks) {
            Long accountId = accountLink.getAccountId();
            accounts.put(accountId, allAccountsMap.get(accountId));
        }

        // fetch services linked both ways
        Map<Long, Long> map1 = consumeMapDao.findConsumedServicesIdsToStackIdsFromOtherAccounts(account.getId());
        Map<Long, Long> map2 = consumeMapDao.findConsumedByServicesIdsToStackIdsFromOtherAccounts(account.getId());
        linkedServicesIds.addAll(map1.keySet());
        linkedStackIds.addAll(map1.values());
        linkedServicesIds.addAll(map2.keySet());
        linkedStackIds.addAll(map2.values());

        return new MetaHelperInfo(account, accounts, linkedServicesIds, linkedStackIds,
                metaDataInfoDao);
    }

    private List<ContainerMetaData> fetchContainersMetadata(MetaHelperInfo helperInfo) {
        List<ContainerMetaData> containersMD = metaDataInfoDao.getManagedContainersData(helperInfo);
        Map<Long, String> instanceIdToUuid = new HashMap<>();
        for (ContainerMetaData containerMd : containersMD) {
            instanceIdToUuid.put(containerMd.getInstanceId(), containerMd.getUuid());
        }
        // add networkFrom containers
        containersMD.addAll(metaDataInfoDao.getNetworkFromContainersData(instanceIdToUuid,
                helperInfo));
        // add host level containers
        containersMD.addAll(metaDataInfoDao.getHostContainersData(helperInfo));
        return containersMD;
    }

    protected List<? extends ServiceConsumeMap> getServiceLinks(MetaHelperInfo helperInfo) {
        List<ServiceConsumeMap> allSvcLinks = objectManager.find(ServiceConsumeMap.class,
                SERVICE_CONSUME_MAP.ACCOUNT_ID,
                helperInfo.getAccount().getId(), SERVICE_CONSUME_MAP.REMOVED, null);

        if (!helperInfo.getOtherAccountsServicesIds().isEmpty()) {
            List<Object> servicesIds = new ArrayList<>();
            servicesIds.addAll(helperInfo.getOtherAccountsServicesIds());
            allSvcLinks.addAll(objectManager.find(ServiceConsumeMap.class,
                    SERVICE_CONSUME_MAP.SERVICE_ID,
                    new Condition(ConditionType.IN, servicesIds),
                    SERVICE_CONSUME_MAP.REMOVED, null));
        }

        return allSvcLinks;
    }

    protected Map<Long, List<ServiceConsumeMap>> getServiceIdToServiceLinks(MetaHelperInfo helperInfo) {
        List<? extends ServiceConsumeMap> allSvcLinks = getServiceLinks(helperInfo);
        Map<Long, List<ServiceConsumeMap>> svcIdToconsumedSvcs = new HashMap<>();
        for (ServiceConsumeMap link : allSvcLinks) {
            List<ServiceConsumeMap> links = svcIdToconsumedSvcs.get(link.getServiceId());
            if (links == null) {
                links = new ArrayList<>();
            }
            links.add(link);
            svcIdToconsumedSvcs.put(link.getServiceId(), links);
        }
        return svcIdToconsumedSvcs;
    }

    protected Map<Long, Service> getServiceIdToService(List<? extends Service> allSvcs) {
        Map<Long, Service> svcIdsToSvc = new HashMap<>();
        for (Service svc : allSvcs) {
            svcIdsToSvc.put(svc.getId(), svc);
        }
        return svcIdsToSvc;
    }

    private static Yaml getYaml() {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        Representer representer = new Representer();
        representer.addClassTag(SelfMetaData.class, Tag.MAP);
        representer.addClassTag(DefaultMetaData.class, Tag.MAP);
        representer.addClassTag(ServiceMetaDataVersion1.class, Tag.MAP);
        representer.addClassTag(ServiceMetaDataVersion2.class, Tag.MAP);
        representer.addClassTag(ServiceMetaDataVersion3.class, Tag.MAP);
        representer.addClassTag(StackMetaDataVersion1.class, Tag.MAP);
        representer.addClassTag(StackMetaDataVersion2.class, Tag.MAP);
        representer.addClassTag(StackMetaDataVersion3.class, Tag.MAP);
        representer.addClassTag(ContainerMetaDataVersion3.class, Tag.MAP);
        Yaml yaml = new Yaml(representer, options);
        return yaml;
    }

    protected String generateYml(Map<String, Object> dataWithVersion) {
        Yaml yaml = getYaml();
        String yamlStr = yaml.dump(dataWithVersion);
        return yamlStr;
    }

    protected Map<String, Object> getFullMetaData(String itemVersion, List<ContainerMetaData> containersMD,
            Map<String, StackMetaData> stackNameToStack, Map<Long, Map<String, ServiceMetaData>> serviceIdToServiceLaunchConfigs,
            Version version, Map<Long, Service> svcIdsToSvc,
            Map<Long, List<ServiceConsumeMap>> svcIdToSvcLinks, long agentHostId,
            MetaHelperInfo helperInfo) {

        // 1. generate containers metadata
        Map<Long, Map<String, List<ContainerMetaData>>> serviceIdToLaunchConfigToContainer = new HashMap<>();
        containersMD = populateContainersData(containersMD, stackNameToStack,
                serviceIdToServiceLaunchConfigs,
                serviceIdToLaunchConfigToContainer, version);

        // 2. generate service metadata based on version + add generated containers to service
        serviceIdToServiceLaunchConfigs = applyVersionToService(serviceIdToServiceLaunchConfigs,
                serviceIdToLaunchConfigToContainer, version, svcIdsToSvc, svcIdToSvcLinks);

        // 3. generate stack metadata based on version + add services generated on the previous step
        stackNameToStack = applyVersionToStack(stackNameToStack, serviceIdToServiceLaunchConfigs, version);

        // 4. populate self section
        Map<String, SelfMetaData> selfMD = new HashMap<>();
        List<String> ipsOnHost = metaDataInfoDao.getPrimaryIpsOnInstanceHost(agentHostId);
        populateSelfSection(containersMD, stackNameToStack, serviceIdToServiceLaunchConfigs, selfMD, version, ipsOnHost,
                agentHostId);

        // 5. get host meta data
        List<HostMetaData> hostsMD = new ArrayList<>();
        for (HostMetaData data : helperInfo.getHostIdToHostMetadata().values()) {
            hostsMD.add(applyVersionToHost(data, version));
        }

        HostMetaData selfHostMD = helperInfo.getHostIdToHostMetadata().get(agentHostId);
        // 6. get networksMetadata
        List<NetworkMetaData> networks = metaDataInfoDao.getNetworksMetaData(helperInfo);

        // 7. full data combined of (n) self sections and default one
        Map<String, Object> fullData = getFullMetaData(itemVersion, containersMD, stackNameToStack,
                serviceIdToServiceLaunchConfigs, selfMD, hostsMD, selfHostMD, networks);

        return fullData;
    }

    protected HostMetaData applyVersionToHost(HostMetaData data, Version version) {
        return HostMetaData.getHostMetaData(data, version);
    }

    protected Map<String, Object> getFullMetaData(String itemVersion, List<ContainerMetaData> containersMD,
            Map<String, StackMetaData> stackNameToStack,
            Map<Long, Map<String, ServiceMetaData>> serviceIdToServiceLaunchConfigs, Map<String, SelfMetaData> selfMD,
            List<HostMetaData> hostsMD, HostMetaData selfHostMD, List<NetworkMetaData> networks) {
        List<StackMetaData> stacksMD = new ArrayList<>();
        for (StackMetaData stack : stackNameToStack.values()) {
            stacksMD.add(stack);
        }
        List<ServiceMetaData> servicesMD = new ArrayList<>();
        for (Map<String, ServiceMetaData> svcs : serviceIdToServiceLaunchConfigs.values()) {
            servicesMD.addAll(svcs.values());
        }

        Map<String, Object> fullData = new HashMap<>();
        fullData.putAll(selfMD);
        fullData.put("default", new DefaultMetaData(itemVersion, containersMD, servicesMD,
                stacksMD, hostsMD, selfHostMD, networks));
        return fullData;
    }

    protected void populateSelfSection(List<ContainerMetaData> containersMD, Map<String, StackMetaData> stackNameToStack,
            Map<Long, Map<String, ServiceMetaData>> serviceIdToServiceLaunchConfigs,
            Map<String, SelfMetaData> selfMD, Version version,
            List<String> ipsOnHost, long hostId) {
        for (ContainerMetaData containerMD : containersMD) {
            if (!ipsOnHost.contains(containerMD.getPrimary_ip())) {
                continue;
            }
            ServiceMetaData svcData = null;
            StackMetaData stackData = null;
            if (containerMD.getServiceId() != null) {
                String configName = containerMD.getDnsPrefix();
                if (configName == null) {
                    configName = ServiceConstants.PRIMARY_LAUNCH_CONFIG_NAME;
                }
                Map<String, ServiceMetaData> svcsData = serviceIdToServiceLaunchConfigs.get(containerMD.getServiceId());
                if (svcsData != null) {
                    svcData = svcsData.get(configName);
                    stackData = stackNameToStack.get(svcData.getStack_name());
                }
            }
            addToSelf(selfMD, containerMD, svcData, stackData, hostId, version);
        }
    }

    protected Map<Long, Map<String, ServiceMetaData>> applyVersionToService(Map<Long, Map<String, ServiceMetaData>> serviceIdToServiceLaunchConfigs,
            Map<Long, Map<String, List<ContainerMetaData>>> serviceIdToLaunchConfigToContainer,
            Version version, Map<Long, Service> svcIdsToSvc, Map<Long, List<ServiceConsumeMap>> svcIdToSvcLinks) {
        Map<Long, Map<String, ServiceMetaData>> newData = new HashMap<>();
        for (Long serviceId : serviceIdToServiceLaunchConfigs.keySet()) {
            Map<String, ServiceMetaData> launchConfigToService = serviceIdToServiceLaunchConfigs.get(serviceId);
            if (launchConfigToService != null) {
                Map<String, ServiceMetaData> launchConfigToServiceModified = new HashMap<>();
                for (String launchConfigName : launchConfigToService.keySet()) {
                    ServiceMetaData svcMDOriginal = launchConfigToService.get(launchConfigName);
                    if (svcMDOriginal != null) {
                        setLinksInfo(serviceIdToServiceLaunchConfigs, svcMDOriginal, svcIdsToSvc, svcIdToSvcLinks);
                        if (serviceIdToLaunchConfigToContainer.get(serviceId) != null) {
                            svcMDOriginal.setContainersObj(serviceIdToLaunchConfigToContainer.get(serviceId)
                                    .get(launchConfigName));
                        }
                        ServiceMetaData serviceMDModified = ServiceMetaData.getServiceMetaData(svcMDOriginal, version);
                        launchConfigToServiceModified.put(launchConfigName, serviceMDModified);
                    }
                }
                newData.put(serviceId, launchConfigToServiceModified);
            }
        }
        return newData;
    }

    protected Map<String, StackMetaData> applyVersionToStack(Map<String, StackMetaData> stackNameToStack,
            Map<Long, Map<String, ServiceMetaData>> serviceIdToServiceLaunchConfigs,
            Version version) {
        // modify stack data
        Map<String, StackMetaData> newData = new HashMap<>();
        for (String stackName : stackNameToStack.keySet()) {
            StackMetaData original = stackNameToStack.get(stackName);
            // add service to stack
            List<ServiceMetaData> services = new ArrayList<>();
            for (Long serviceId : serviceIdToServiceLaunchConfigs.keySet()) {
                for (ServiceMetaData service : serviceIdToServiceLaunchConfigs.get(serviceId).values()) {
                    if (service.getStackId().equals(original.getId())) {
                        services.add(service);
                    }
                }
            }
            original.setServicesObj(services);
            StackMetaData stackMDModified = StackMetaData.getStackMetaData(original, version);
            newData.put(stackMDModified.getName(), stackMDModified);
        }
        return newData;
    }

    protected List<ContainerMetaData> populateContainersData(List<ContainerMetaData> containersMD, Map<String, StackMetaData> stackNameToStack,
            Map<Long, Map<String, ServiceMetaData>> serviceIdToServiceLaunchConfigs,
            Map<Long, Map<String, List<ContainerMetaData>>> serviceIdToLaunchConfigToContainer, Version version) {
        List<ContainerMetaData> newData = new ArrayList<>();
        for (ContainerMetaData containerMD : containersMD) {
            if (containerMD.getServiceId() != null) {
                String configName = containerMD.getDnsPrefix();
                if (configName == null) {
                    configName = ServiceConstants.PRIMARY_LAUNCH_CONFIG_NAME;
                }
                Map<String, ServiceMetaData> svcsData = serviceIdToServiceLaunchConfigs.get(containerMD.getServiceId());
                if (svcsData != null) {
                    ServiceMetaData svcData = svcsData.get(configName);
                    if (svcData == null) {
                        continue;
                    }
                    containerMD.setStack_name(svcData.getStack_name());
                    containerMD.setStack_uuid(svcData.getStack_uuid());
                    containerMD.setService_name(svcData.getName());
                    // apply version to a container (for service containers)
                    containerMD = ContainerMetaData.getContainerMetaData(containerMD, version);

                    Map<String, List<ContainerMetaData>> launchConfigToContainer = serviceIdToLaunchConfigToContainer
                            .get(containerMD.getServiceId());
                    if (launchConfigToContainer == null) {
                        launchConfigToContainer = new HashMap<>();
                    }

                    List<ContainerMetaData> serviceContainers = launchConfigToContainer.get(configName);
                    if (serviceContainers == null) {
                        serviceContainers = new ArrayList<>();
                    }
                    serviceContainers.add(containerMD);
                    launchConfigToContainer.put(configName, serviceContainers);
                    serviceIdToLaunchConfigToContainer.put(containerMD.getServiceId(), launchConfigToContainer);
                }
            } else {
                // apply version to a container for standalone containers
                containerMD = ContainerMetaData.getContainerMetaData(containerMD, version);
            }
            newData.add(containerMD);
        }
        return newData;
    }

    protected List<? extends Stack> getStacks(MetaHelperInfo helperInfo) {
        List<Stack> stacks = objectManager.find(Stack.class, STACK.ACCOUNT_ID,
                helperInfo.getAccount().getId(), STACK.REMOVED, null);
        if (!helperInfo.getOtherAccountsStackIds().isEmpty()) {
            List<Object> stackIds = new ArrayList<>();
            stackIds.addAll(helperInfo.getOtherAccountsStackIds());
            stacks.addAll(objectManager.find(Stack.class, STACK.ID,
                    new Condition(ConditionType.IN, stackIds), STACK.REMOVED, null));
        }

        return stacks;
    }

    protected void populateStacksServicesInfo(MetaHelperInfo helperInfo, Map<String, StackMetaData> stacksMD,
            Map<Long, Map<String, ServiceMetaData>> servicesMD, List<? extends Service> allSvcs) {
        List<? extends Stack> stacks = getStacks(helperInfo);
        Map<Long, List<Service>> envIdToService = new HashMap<>();
        for (Service svc : allSvcs) {
            List<Service> envSvcs = envIdToService.get(svc.getStackId());
            if (envSvcs == null) {
                envSvcs = new ArrayList<>();
            }
            envSvcs.add(svc);
            envIdToService.put(svc.getStackId(), envSvcs);
        }
        for (Stack stack : stacks) {
            StackMetaData stackMetaData = new StackMetaData(stack, helperInfo.getAccounts().get(stack.getAccountId()));
            List<Service> services = envIdToService.get(stack.getId());
            if (services == null) {
                services = new ArrayList<>();
            }
            List<ServiceMetaData> stackServicesMD = getServicesInfo(stack, services, helperInfo.getAccounts());
            stacksMD.put(stackMetaData.getName(), stackMetaData);
            for (ServiceMetaData stackServiceMD : stackServicesMD) {
                Map<String, ServiceMetaData> launchConfigToSvcMap = servicesMD.get(stackServiceMD.getServiceId());
                if (launchConfigToSvcMap == null) {
                    launchConfigToSvcMap = new HashMap<>();
                }
                if (stackServiceMD.isPrimaryConfig()) {
                    launchConfigToSvcMap.put(ServiceConstants.PRIMARY_LAUNCH_CONFIG_NAME, stackServiceMD);
                } else {
                    launchConfigToSvcMap.put(stackServiceMD.getName(), stackServiceMD);
                }
                servicesMD.put(stackServiceMD.getServiceId(), launchConfigToSvcMap);
            }
        }
    }

    protected void addToSelf(Map<String, SelfMetaData> self, ContainerMetaData containerMD,
            ServiceMetaData serviceMD, StackMetaData stackMD, long hostId, Version version) {
        if (containerMD.getPrimary_ip() == null) {
            return;
        }

        if (containerMD.getHostMetaData() == null) {
            return;
        }

        if (containerMD.getHostMetaData().getHostId().equals(hostId)) {
            self.put(containerMD.getPrimary_ip(), new SelfMetaData(containerMD, serviceMD,
                    stackMD, containerMD.getHostMetaData(), version));
        }
    }


    protected List<ServiceMetaData> getServicesInfo(Stack env, List<Service> services, Map<Long, Account> accounts) {
        List<ServiceMetaData> stackServicesMD = new ArrayList<>();
        Map<Long, Service> idToService = new HashMap<>();
        for (Service service : services) {
            List<ContainerMetaData> serviceContainersMD = new ArrayList<>();
            getServiceInfo(serviceContainersMD, env, stackServicesMD, idToService, service, accounts);
        }
        return stackServicesMD;
    }

    protected void getServiceInfo(List<ContainerMetaData> serviceContainersMD, Stack env,
            List<ServiceMetaData> stackServices, Map<Long, Service> idToService, Service service,
            Map<Long, Account> accounts) {
        idToService.put(service.getId(), service);
        List<String> launchConfigNames = ServiceDiscoveryUtil.getServiceLaunchConfigNames(service);
        if (launchConfigNames.isEmpty()) {
            launchConfigNames.add(ServiceConstants.PRIMARY_LAUNCH_CONFIG_NAME);
        }
        for (String launchConfigName : launchConfigNames) {
            getLaunchConfigInfo(env, stackServices, idToService, service, launchConfigNames, launchConfigName, accounts);
        }
    }

    protected void getLaunchConfigInfo(Stack env, List<ServiceMetaData> stackServices,
            Map<Long, Service> idToService, Service service, List<String> launchConfigNames,
            String launchConfigName, Map<Long, Account> accounts) {
        boolean isPrimaryConfig = launchConfigName
                .equalsIgnoreCase(ServiceConstants.PRIMARY_LAUNCH_CONFIG_NAME);
        String serviceName = isPrimaryConfig ? service.getName()
                : launchConfigName;
        List<String> sidekicks = new ArrayList<>();

        if (isPrimaryConfig) {
            getSidekicksInfo(service, sidekicks, launchConfigNames);
        }

        LBConfigMetadataStyle lbConfig = lbInfoDao.generateLBConfigMetadataStyle(service);
        Object hcO = null;
        if (service.getKind().equalsIgnoreCase(ServiceConstants.KIND_EXTERNAL_SERVICE)) {
            hcO = DataAccessor.field(service, InstanceConstants.FIELD_HEALTH_CHECK, Object.class);
        } else {
            hcO = ServiceDiscoveryUtil.getLaunchConfigObject(service, launchConfigName,
                    InstanceConstants.FIELD_HEALTH_CHECK);
        }

        InstanceHealthCheck hc = null;
        if (hcO != null) {
            hc = jsonMapper.convertValue(hcO, InstanceHealthCheck.class);
        }
        ServiceMetaData svcMetaData = new ServiceMetaData(service, serviceName, env, sidekicks, hc, lbConfig,
                accounts.get(env.getAccountId()));
        stackServices.add(svcMetaData);
    }

    protected void getSidekicksInfo(Service service, List<String> sidekicks, List<String> launchConfigNames) {
        for (String launchConfigName : launchConfigNames) {
            if (!launchConfigName.equalsIgnoreCase(ServiceConstants.PRIMARY_LAUNCH_CONFIG_NAME)) {
                sidekicks.add(launchConfigName);
            }
        }
    }

    protected void setLinksInfo(Map<Long, Map<String, ServiceMetaData>> services,
            ServiceMetaData serviceMD, Map<Long, Service> svcIdsToSvc,
            Map<Long, List<ServiceConsumeMap>> svcIdToSvcLinks) {
        Map<String, String> links = new HashMap<>();
        List<? extends ServiceConsumeMap> consumeMaps = svcIdToSvcLinks.get(serviceMD.getServiceId());
        if (consumeMaps != null) {
            for (ServiceConsumeMap consumedMap : consumeMaps) {
                Service service = svcIdsToSvc.get(consumedMap.getConsumedServiceId());
                if (service == null) {
                    continue;
                }
                Map<String, ServiceMetaData> consumedService = services.get(service.getId());
                if (consumedService == null) {
                    continue;
                }
                ServiceMetaData consumedServiceData = consumedService.get(
                        serviceMD.getLaunchConfigName());
                String linkAlias = ServiceDiscoveryUtil.getDnsName(service, consumedMap, null, false);
                if (consumedServiceData != null) {
                    links.put(
                            consumedServiceData.getStack_name() + "/" + consumedServiceData.getName(), linkAlias);
                }
            }
        }

        serviceMD.setLinks(links);
    }


}
