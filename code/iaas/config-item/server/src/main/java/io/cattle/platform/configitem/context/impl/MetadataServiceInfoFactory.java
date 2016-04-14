package io.cattle.platform.configitem.context.impl;

import static io.cattle.platform.core.model.tables.ServiceConsumeMapTable.*;
import io.cattle.platform.configitem.context.dao.MetaDataInfoDao;
import io.cattle.platform.configitem.context.dao.MetaDataInfoDao.Version;
import io.cattle.platform.configitem.context.data.metadata.common.ContainerMetaData;
import io.cattle.platform.configitem.context.data.metadata.common.HostMetaData;
import io.cattle.platform.configitem.context.data.metadata.common.ServiceMetaData;
import io.cattle.platform.configitem.context.data.metadata.common.StackMetaData;
import io.cattle.platform.configitem.context.data.metadata.version1.ServiceMetaDataVersionTemp;
import io.cattle.platform.configitem.context.data.metadata.version1.StackMetaDataVersionTemp;
import io.cattle.platform.configitem.server.model.ConfigItem;
import io.cattle.platform.configitem.server.model.Request;
import io.cattle.platform.configitem.server.model.impl.ArchiveContext;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.Environment;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.ServiceConsumeMap;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.servicediscovery.api.constants.ServiceDiscoveryConstants;
import io.cattle.platform.servicediscovery.api.util.ServiceDiscoveryUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;

@Named
public class MetadataServiceInfoFactory extends AbstractAgentBaseContextFactory {

    @Inject
    MetaDataInfoDao metadataInfoDao;

    @Override
    protected void populateContext(Agent agent, Instance instance, ConfigItem item, ArchiveContext context, Request configRequest) {
        Account account = objectManager.loadResource(Account.class, instance.getAccountId());
        Object currentRev = configRequest.getParams().get(ServiceDiscoveryConstants.FIELD_METADATA_REVISION);
        Long currentRevision = currentRev != null ? Long.valueOf(((String[]) currentRev)[0]) : -1;
        Long requestedRevision = account.getMetadataRevision();
        context.getData().put("params",
                ServiceDiscoveryConstants.FIELD_METADATA_REVISION + "=" + requestedRevision);
        if (currentRevision.equals(requestedRevision)) {
            return;
        }
        // get all the objects who's metadata revision is greater than requested revision
        // and <= account metadata revision
        Map<String, Map<String, String>> selfUuids = new HashMap<>();
        Map<String, Object> typeToData = new HashMap<>();
        populateMetadata(account, instance, context, currentRevision, requestedRevision, typeToData, selfUuids);
        context.getData().put("newdata", generateYml(typeToData));
        context.getData().put("self", generateYml(selfUuids));
        context.getReplaceInPath().put("data.yml", "data_" + requestedRevision + ".yml");
        context.getReplaceInPath().put("self.yml", "self_" + requestedRevision + ".yml");
    }

    protected Map<String, Object> populateMetadata(Account account, Instance instance,
            ArchiveContext context, Long currentRevision, Long requestedRevision, Map<String, Object> typeToData,
            Map<String, Map<String, String>> selfUuids) {
        List<ContainerMetaData> containersMD = metadataInfoDao.getContainersData(account.getId(), currentRevision,
                requestedRevision);
        Map<String, StackMetaData> stackNameToStack = new HashMap<>();
        Map<Long, Map<String, ServiceMetaData>> serviceIdToServiceLaunchConfigs = new HashMap<>();
        List<? extends Service> allSvcs = metadataInfoDao.getServices(account.getId());

        Map<Long, Service> svcIdsToSvc = getServiceIdToService(allSvcs);
        Map<Long, List<ServiceConsumeMap>> svcIdToSvcLinks = getServiceIdToServiceLinks(account);
        populateStacksServicesInfo(account, stackNameToStack, serviceIdToServiceLaunchConfigs, allSvcs,
                currentRevision, requestedRevision);
        // 1. generate containers metadata
        Map<Long, Map<String, List<ContainerMetaData>>> serviceIdToLaunchConfigToContainer = generateContainersMetadata(
                instance, typeToData, selfUuids, containersMD, stackNameToStack, serviceIdToServiceLaunchConfigs);

        // 2. generate service metadata based on version + add generated containers to service
        generateServicesMetadata(typeToData, serviceIdToServiceLaunchConfigs, svcIdsToSvc, svcIdToSvcLinks,
                serviceIdToLaunchConfigToContainer);

        // 3. generate stack metadata based on version + add services generated on the previous step
        generateStackMetadata(typeToData, stackNameToStack, serviceIdToServiceLaunchConfigs);

        // 4. get host meta data
        generateHostMetadata(instance, currentRevision, requestedRevision, typeToData);
        return typeToData;
    }

    protected void generateHostMetadata(Instance instance, Long currentRevision, Long requestedRevision,
            Map<String, Object> typeToData) {
        Map<Long, HostMetaData> hostIdToHost = metadataInfoDao.getHostIdToHostMetadata(instance.getAccountId(),
                currentRevision, requestedRevision);
        List<HostMetaData> hostsMD = new ArrayList<>(hostIdToHost.values());
        List<HostMetaData> selfHostMD = metadataInfoDao.getInstanceHostMetaData(instance,
                currentRevision, requestedRevision);
        typeToData.put("hosts", hostsMD);
        typeToData.put("host", !selfHostMD.isEmpty() ? selfHostMD.get(0) : null);
    }

    protected void generateStackMetadata(Map<String, Object> typeToData, Map<String, StackMetaData> stackNameToStack,
            Map<Long, Map<String, ServiceMetaData>> serviceIdToServiceLaunchConfigs) {
        List<StackMetaData> stacksMd = generateStackMetadata(stackNameToStack, serviceIdToServiceLaunchConfigs);
        List<StackMetaData> finalData = new ArrayList<>();
        for (StackMetaData stack : stacksMd) {
            if (stack.isIncludeToData()) {
                finalData.add(stack);
            }
        }
        typeToData.put("stacks", finalData);
    }

    protected void generateServicesMetadata(Map<String, Object> typeToData,
            Map<Long, Map<String, ServiceMetaData>> serviceIdToServiceLaunchConfigs, Map<Long, Service> svcIdsToSvc,
            Map<Long, List<ServiceConsumeMap>> svcIdToSvcLinks,
            Map<Long, Map<String, List<ContainerMetaData>>> serviceIdToLaunchConfigToContainer) {
        List<ServiceMetaData> servicesMd = generateServiceMetadata(serviceIdToServiceLaunchConfigs,
                serviceIdToLaunchConfigToContainer, svcIdsToSvc, svcIdToSvcLinks);
        List<ServiceMetaData> finalData = new ArrayList<>();
        for (ServiceMetaData svc : servicesMd) {
            if (svc.isIncludeToData()) {
                finalData.add(svc);
            }
        }
        typeToData.put("services", finalData);
    }

    protected Map<Long, Map<String, List<ContainerMetaData>>> generateContainersMetadata(Instance instance,
            Map<String, Object> typeToData, Map<String, Map<String, String>> selfUuids,
            List<ContainerMetaData> containersMD, Map<String, StackMetaData> stackNameToStack,
            Map<Long, Map<String, ServiceMetaData>> serviceIdToServiceLaunchConfigs) {
        Map<Long, Map<String, List<ContainerMetaData>>> serviceIdToLaunchConfigToContainer = new HashMap<>();
        containersMD = populateContainersData(instance, containersMD, stackNameToStack,
                serviceIdToServiceLaunchConfigs,
                serviceIdToLaunchConfigToContainer);
        addToSelf(selfUuids, containersMD, stackNameToStack, serviceIdToServiceLaunchConfigs);
        List<ContainerMetaData> finalData = new ArrayList<>();
        for (ContainerMetaData cData : containersMD) {
            if (cData.isIncludeToData()) {
                finalData.add(cData);
            }
        }
        typeToData.put("containers", finalData);
        return serviceIdToLaunchConfigToContainer;
    }

    protected void addToSelf(Map<String, Map<String, String>> selfUuids, List<ContainerMetaData> containersMD,
            Map<String, StackMetaData> stackNameToStack,
            Map<Long, Map<String, ServiceMetaData>> serviceIdToServiceLaunchConfigs) {
        for (ContainerMetaData c : containersMD) {
            if (!c.isIncludeToData()) {
                continue;
            }
            List<String> removedStates = Arrays.asList(CommonStatesConstants.REMOVED, CommonStatesConstants.REMOVING);
            if (removedStates.contains(c.getState())) {
                continue;
            }
            Map<String, String> selfData = selfUuids.get(c.getPrimary_ip());
            if (selfData == null) {
                selfData = new HashMap<>();
            }
            selfData.put("container", c.getMetadataUuid());
            if (c.getServiceId() != null) {
                ServiceMetaData svcData = null;
                StackMetaData stackData = null;
                String configName = c.getDnsPrefix();
                if (configName == null) {
                    configName = ServiceDiscoveryConstants.PRIMARY_LAUNCH_CONFIG_NAME;
                }
                Map<String, ServiceMetaData> svcsData = serviceIdToServiceLaunchConfigs.get(c.getServiceId());
                if (svcsData != null) {
                    svcData = svcsData.get(configName);
                    stackData = stackNameToStack.get(svcData.getStack_name());
                    selfData.put("service", svcData.getMetadataUuid());
                    selfData.put("stack", stackData.getMetadataUuid());
                }

                HostMetaData hostData = c.getHostMetaData();
                if (hostData != null) {
                    selfData.put("host", hostData.getMetadataUuid());
                }
            }

            selfUuids.put(c.getPrimary_ip(), selfData);
        }
    }

    protected String generateYml(Object object) {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        Representer representer = new Representer();
        representer.addClassTag(ContainerMetaData.class, Tag.MAP);
        representer.addClassTag(HostMetaData.class, Tag.MAP);
        representer.addClassTag(ServiceMetaDataVersionTemp.class, Tag.MAP);
        representer.addClassTag(StackMetaDataVersionTemp.class, Tag.MAP);
        Yaml yaml = new Yaml(representer, options);
        String yamlStr = yaml.dump(object);
        return yamlStr;
    }

    protected Map<Long, List<ServiceConsumeMap>> getServiceIdToServiceLinks(Account account) {
        List<? extends ServiceConsumeMap> allSvcLinks = objectManager.find(ServiceConsumeMap.class,
                SERVICE_CONSUME_MAP.ACCOUNT_ID,
                account.getId(), SERVICE_CONSUME_MAP.REMOVED, null);
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

    protected List<ServiceMetaData> generateServiceMetadata(
            Map<Long, Map<String, ServiceMetaData>> serviceIdToServiceLaunchConfigs,
            Map<Long, Map<String, List<ContainerMetaData>>> serviceIdToLaunchConfigToContainer,
            Map<Long, Service> svcIdsToSvc, Map<Long, List<ServiceConsumeMap>> svcIdToSvcLinks) {
        List<ServiceMetaData> newData = new ArrayList<>();
        for (Long serviceId : serviceIdToServiceLaunchConfigs.keySet()) {
            Map<String, ServiceMetaData> launchConfigToService = serviceIdToServiceLaunchConfigs.get(serviceId);
            if (launchConfigToService != null) {
                for (String launchConfigName : launchConfigToService.keySet()) {
                    ServiceMetaData svcMDOriginal = launchConfigToService.get(launchConfigName);
                    if (svcMDOriginal != null) {
                        setLinksInfo(serviceIdToServiceLaunchConfigs, svcMDOriginal, svcIdsToSvc, svcIdToSvcLinks);
                        if (serviceIdToLaunchConfigToContainer.get(serviceId) != null) {
                            svcMDOriginal.setContainersObj(serviceIdToLaunchConfigToContainer.get(serviceId)
                                    .get(launchConfigName));
                        }
                        ServiceMetaData serviceMDModified = ServiceMetaData.getServiceMetaData(svcMDOriginal,
                                Version.tempVersion);
                        newData.add(serviceMDModified);
                    }
                }
            }
        }
        return newData;
    }

    protected List<StackMetaData> generateStackMetadata(Map<String, StackMetaData> stackNameToStack,
            Map<Long, Map<String, ServiceMetaData>> serviceIdToServiceLaunchConfigs) {
        // modify stack data
        List<StackMetaData> newData = new ArrayList<>();
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
            StackMetaData stackMDModified = StackMetaData.getStackMetaData(original, Version.tempVersion);
            newData.add(stackMDModified);
        }
        return newData;
    }

    protected List<ContainerMetaData> populateContainersData(Instance instance, List<ContainerMetaData> containersMD,
            Map<String, StackMetaData> stackNameToStack,
            Map<Long, Map<String, ServiceMetaData>> serviceIdToServiceLaunchConfigs,
            Map<Long, Map<String, List<ContainerMetaData>>> serviceIdToLaunchConfigToContainer) {
        List<ContainerMetaData> newData = new ArrayList<>();
        for (ContainerMetaData containerMD : containersMD) {
            if (containerMD.getServiceId() != null) {
                String configName = containerMD.getDnsPrefix();
                if (configName == null) {
                    configName = ServiceDiscoveryConstants.PRIMARY_LAUNCH_CONFIG_NAME;
                }
                Map<String, ServiceMetaData> svcsData = serviceIdToServiceLaunchConfigs.get(containerMD.getServiceId());
                if (svcsData != null) {
                    ServiceMetaData svcData = svcsData.get(configName);
                    containerMD.setStack_name(svcData.getStack_name());
                    containerMD.setService_name(svcData.getName());

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
            }
            newData.add(containerMD);
        }
        return newData;
    }

    protected void populateStacksServicesInfo(Account account, Map<String, StackMetaData> stacksMD,
            Map<Long, Map<String, ServiceMetaData>> servicesMD, List<? extends Service> allSvcs, Long currentRevision,
            Long requestedRevision) {
        List<? extends Environment> envs = metadataInfoDao.getStacks(account.getId());
        Map<Long, List<Service>> envIdToService = new HashMap<>();
        for (Service svc : allSvcs) {
            List<Service> envSvcs = envIdToService.get(svc.getEnvironmentId());
            if (envSvcs == null) {
                envSvcs = new ArrayList<>();
            }
            envSvcs.add(svc);
            envIdToService.put(svc.getEnvironmentId(), envSvcs);
        }
        for (Environment env : envs) {
            boolean includeToData = false;
            if (env.getRevision() > currentRevision && env.getRevision() <= requestedRevision) {
                includeToData = true;
            }
            StackMetaData stackMetaData = new StackMetaData(env, account, includeToData);
            List<Service> services = envIdToService.get(env.getId());
            if (services == null) {
                services = new ArrayList<>();
            }
            List<ServiceMetaData> stackServicesMD = getServicesInfo(env, account, services, currentRevision,
                    requestedRevision);
            stacksMD.put(stackMetaData.getName(), stackMetaData);
            for (ServiceMetaData stackServiceMD : stackServicesMD) {
                Map<String, ServiceMetaData> launchConfigToSvcMap = servicesMD.get(stackServiceMD.getServiceId());
                if (launchConfigToSvcMap == null) {
                    launchConfigToSvcMap = new HashMap<>();
                }
                if (stackServiceMD.isPrimaryConfig()) {
                    launchConfigToSvcMap.put(ServiceDiscoveryConstants.PRIMARY_LAUNCH_CONFIG_NAME, stackServiceMD);
                } else {
                    launchConfigToSvcMap.put(stackServiceMD.getName(), stackServiceMD);
                }
                servicesMD.put(stackServiceMD.getServiceId(), launchConfigToSvcMap);
            }
        }
    }

    protected List<ServiceMetaData> getServicesInfo(Environment env, Account account, List<Service> services,
            Long currentRevision,
            Long requestedRevision) {
        List<ServiceMetaData> stackServicesMD = new ArrayList<>();
        Map<Long, Service> idToService = new HashMap<>();
        for (Service service : services) {
            List<ContainerMetaData> serviceContainersMD = new ArrayList<>();
            getServiceInfo(account, serviceContainersMD, env, stackServicesMD, idToService, service, currentRevision,
                    requestedRevision);
        }
        return stackServicesMD;
    }

    protected void getServiceInfo(Account account, List<ContainerMetaData> serviceContainersMD,
            Environment env, List<ServiceMetaData> stackServices, Map<Long, Service> idToService,
            Service service, Long currentRevision,
            Long requestedRevision) {
        idToService.put(service.getId(), service);
        List<String> launchConfigNames = ServiceDiscoveryUtil.getServiceLaunchConfigNames(service);
        if (launchConfigNames.isEmpty()) {
            launchConfigNames.add(ServiceDiscoveryConstants.PRIMARY_LAUNCH_CONFIG_NAME);
        }
        for (String launchConfigName : launchConfigNames) {
            getLaunchConfigInfo(account, env, stackServices, idToService, service, launchConfigNames,
                    launchConfigName, currentRevision, requestedRevision);
        }
    }

    @SuppressWarnings("unchecked")
    protected void getLaunchConfigInfo(Account account, Environment env,
            List<ServiceMetaData> stackServices, Map<Long, Service> idToService, Service service,
            List<String> launchConfigNames, String launchConfigName, Long currentRevision,
            Long requestedRevision) {
        boolean isPrimaryConfig = launchConfigName
                .equalsIgnoreCase(ServiceDiscoveryConstants.PRIMARY_LAUNCH_CONFIG_NAME);
        String serviceName = isPrimaryConfig ? service.getName()
                : launchConfigName;
        List<String> sidekicks = new ArrayList<>();

        if (isPrimaryConfig) {
            getSidekicksInfo(service, sidekicks, launchConfigNames);
        }
        Map<String, Object> metadata = DataAccessor.fields(service).withKey(ServiceDiscoveryConstants.FIELD_METADATA)
                .withDefault(Collections.EMPTY_MAP).as(Map.class);
        boolean includeToData = false;
        if (service.getRevision() > currentRevision && service.getRevision() <= requestedRevision) {
            includeToData = true;
        }
        ServiceMetaData svcMetaData = new ServiceMetaData(service, serviceName, env, sidekicks, metadata, includeToData);
        stackServices.add(svcMetaData);
    }

    protected void getSidekicksInfo(Service service, List<String> sidekicks, List<String> launchConfigNames) {
        for (String launchConfigName : launchConfigNames) {
            if (!launchConfigName.equalsIgnoreCase(ServiceDiscoveryConstants.PRIMARY_LAUNCH_CONFIG_NAME)) {
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

