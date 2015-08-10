package io.cattle.platform.configitem.context.impl;

import static io.cattle.platform.core.model.tables.EnvironmentTable.ENVIRONMENT;
import static io.cattle.platform.core.model.tables.ServiceTable.SERVICE;
import io.cattle.platform.configitem.context.dao.MetaDataInfoDao;
import io.cattle.platform.configitem.context.data.ContainerMetaData;
import io.cattle.platform.configitem.context.data.SelfMetaData;
import io.cattle.platform.configitem.context.data.ServiceMetaData;
import io.cattle.platform.configitem.context.data.StackMetaData;
import io.cattle.platform.configitem.server.model.ConfigItem;
import io.cattle.platform.configitem.server.model.impl.ArchiveContext;
import io.cattle.platform.core.dao.GenericMapDao;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.Environment;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.InstanceHostMap;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.ServiceConsumeMap;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.servicediscovery.api.constants.ServiceDiscoveryConstants;
import io.cattle.platform.servicediscovery.api.dao.ServiceConsumeMapDao;
import io.cattle.platform.servicediscovery.api.util.ServiceDiscoveryUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.TransformerUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class ServiceMetadataInfoFactory extends AbstractAgentBaseContextFactory {
    private static final Logger log = LoggerFactory.getLogger(ServiceMetadataInfoFactory.class);

    @Inject
    JsonMapper jsonMapper;

    @Inject
    ServiceConsumeMapDao consumeMapDao;

    @Inject
    MetaDataInfoDao metaDataInfoDao;

    @Inject
    GenericMapDao mapDao;

    @Override
    protected void populateContext(Agent agent, Instance instance, ConfigItem item, ArchiveContext context) {
        Account account = objectManager.loadResource(Account.class, instance.getAccountId());
        List<ContainerMetaData> containersMetaData = metaDataInfoDao.getServicesContainersData(account.getId());
        containersMetaData.addAll(metaDataInfoDao.getStandaloneContainersData(account.getId()));

        Map<String, StackMetaData> stacks = new HashMap<>();
        Map<Long, Map<String, ServiceMetaData>> services = new HashMap<>();
        Map<String, SelfMetaData> self = new HashMap<>();
        populateStacksServicesInfo(account, stacks, services);
        for (ContainerMetaData containerMetaData : containersMetaData) {
            ServiceMetaData svcData = null;
            if (containerMetaData.getServiceId() != null) {
                String configName = containerMetaData.getDnsPrefix();
                if (configName == null) {
                    configName = ServiceDiscoveryConstants.PRIMARY_LAUNCH_CONFIG_NAME;
                }
                Map<String, ServiceMetaData> svcsData = services.get(containerMetaData.getServiceId());
                if (svcsData != null) {
                    svcData = svcsData.get(configName);
                    containerMetaData.setStack_name(svcData.getName());
                    containerMetaData.setService_name(svcData.getStack_name());
                    addContainerToSelf(self, containerMetaData, svcData, stacks.get(svcData.getStack_name()), getInstanceHostId(instance));
                    svcData.addToContainer(containerMetaData.getName());
                }
            }

            if (svcData == null) {
                addContainerToSelf(self, containerMetaData, null, null, getInstanceHostId(instance));
            }
        }
        
        List<ServiceMetaData> servicesMD = new ArrayList<>();
        for (Map<String, ServiceMetaData> service : services.values()) {
            servicesMD.addAll(service.values());
        }

        List<StackMetaData> stacksMD = new ArrayList<>();
        for (StackMetaData stack : stacks.values()) {
            stacksMD.add(stack);
        }

        try {
            Map<String, String> ipToSelfJsonBlob = new HashMap<>();
            for (String ip : self.keySet()) {
                ipToSelfJsonBlob.put(ip, jsonMapper.writeValueAsString(self.get(ip)));
            }
            context.getData().put("self", ipToSelfJsonBlob);
            context.getData().put("containers", jsonMapper.writeValueAsString(containersMetaData));
            context.getData().put("services", jsonMapper.writeValueAsString(servicesMD));
            context.getData().put("stacks", jsonMapper.writeValueAsString(stacksMD));

        } catch (IOException e) {
            log.error("Failed to marshal service metadata", e);
        }
    }

    protected void populateStacksServicesInfo(Account account, Map<String, StackMetaData> stacksMD,
            Map<Long, Map<String, ServiceMetaData>> servicesMD) {
        List<? extends Environment> envs = objectManager.find(Environment.class, ENVIRONMENT.ACCOUNT_ID,
                account.getId(), ENVIRONMENT.REMOVED, null);
        for (Environment env : envs) {
            List<ContainerMetaData> stackContainersMD = new ArrayList<>();
            List<ServiceMetaData> stackServicesMD = new ArrayList<>();
            List<String> stackServicesNames = getServicesInfo(stackServicesMD, stackContainersMD, env,
                    account);
            StackMetaData stackMetaData = new StackMetaData(env, account, stackServicesNames);
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

    protected void addContainerToSelf(Map<String, SelfMetaData> self, ContainerMetaData containerMetaData,
            ServiceMetaData serviceMetaData, StackMetaData stackMetaData, long hostId) {
        if (containerMetaData.getPrimary_ip() == null) {
            return;
        }

        if (containerMetaData.getHostMetaData() == null) {
            return;
        }

        if (containerMetaData.getHostMetaData().getHostId().equals(hostId)) {
            self.put(containerMetaData.getPrimary_ip(), new SelfMetaData(containerMetaData, serviceMetaData,
                    stackMetaData, containerMetaData.getHostMetaData()));
        }
    }


    @SuppressWarnings("unchecked")
    protected List<String> getServicesInfo(List<ServiceMetaData> stackServicesMD,
            List<ContainerMetaData> stackContainersMD,
            Environment env, Account account) {
        List<? extends Service> services = objectManager.find(Service.class, SERVICE.ENVIRONMENT_ID,
                env.getId(), SERVICE.REMOVED, null);
        Map<Long, Service> idToService = new HashMap<>();
        for (Service service : services) {
            List<ContainerMetaData> serviceContainersMD = new ArrayList<>();
            getServiceInfo(account, serviceContainersMD, env, stackServicesMD, idToService, service);
            stackContainersMD.addAll(serviceContainersMD);
        }
        List<String> stackServicesNames = (List<String>) CollectionUtils.collect(stackServicesMD,
                TransformerUtils.invokerTransformer("getName"));
        return stackServicesNames;
    }

    protected void getServiceInfo(Account account, List<ContainerMetaData> serviceContainersMD,
            Environment env, List<ServiceMetaData> stackServices, Map<Long, Service> idToService,
            Service service) {
        idToService.put(service.getId(), service);
        List<String> launchConfigNames = ServiceDiscoveryUtil.getServiceLaunchConfigNames(service);
        if (launchConfigNames.isEmpty()) {
            launchConfigNames.add(ServiceDiscoveryConstants.PRIMARY_LAUNCH_CONFIG_NAME);
        }
        for (String launchConfigName : launchConfigNames) {
            List<ContainerMetaData> lanchConfigContainersMD = new ArrayList<>();
            getLaunchConfigInfo(account, lanchConfigContainersMD, env, stackServices, idToService, service,
                    launchConfigNames, launchConfigName);
            serviceContainersMD.addAll(lanchConfigContainersMD);
        }
    }

    protected void getLaunchConfigInfo(Account account, List<ContainerMetaData> lanchConfigContainersMD,
            Environment env, List<ServiceMetaData> stackServices, Map<Long, Service> idToService,
            Service service, List<String> launchConfigNames, String launchConfigName) {
        boolean isPrimaryConfig = launchConfigName
                .equalsIgnoreCase(ServiceDiscoveryConstants.PRIMARY_LAUNCH_CONFIG_NAME);
        String serviceName = isPrimaryConfig ? service.getName()
                : launchConfigName;
        List<String> sidekicks = new ArrayList<>();
        Map<String, String> links = new HashMap<>();
        
        if (isPrimaryConfig) {
            getLinksInfo(idToService, service, links);
            getSidekicksInfo(service, sidekicks, launchConfigNames);
        }

        ServiceMetaData svcMetaData = new ServiceMetaData(service, serviceName, env, links, sidekicks);
        stackServices.add(svcMetaData);
    }

    protected void getSidekicksInfo(Service service, List<String> sidekicks, List<String> launchConfigNames) {
        for (String launchConfigName : launchConfigNames) {
            if (!launchConfigName.equalsIgnoreCase(ServiceDiscoveryConstants.PRIMARY_LAUNCH_CONFIG_NAME)) {
                sidekicks.add(launchConfigName);
            }
        }
    }

    protected void getLinksInfo(Map<Long, Service> idToService, Service service, Map<String, String> links) {
        List<? extends ServiceConsumeMap> consumedMaps = consumeMapDao.findConsumedServices(service.getId());
        for (ServiceConsumeMap consumedMap : consumedMaps) {
            Service consumedService = idToService.get(consumedMap.getConsumedServiceId());
            if (consumedService == null) {
                consumedService = objectManager.loadResource(Service.class, consumedMap.getConsumedServiceId());
                idToService.put(consumedService.getId(), consumedService);
            }
            links.put(ServiceDiscoveryUtil.getDnsName(service, consumedMap, null, false), consumedService.getName());
        }
    }

    protected long getInstanceHostId(Instance instance) {
        List<? extends InstanceHostMap> maps = mapDao.findNonRemoved(InstanceHostMap.class, Instance.class,
                instance.getId());
        if (!maps.isEmpty()) {
            return maps.get(0).getHostId();
        }
        return 0;
    }

}
