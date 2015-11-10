package io.cattle.platform.servicediscovery.api.service.impl;

import static io.cattle.platform.core.model.tables.InstanceTable.INSTANCE;
import static io.cattle.platform.core.model.tables.ServiceTable.SERVICE;
import io.cattle.platform.allocator.service.AllocatorService;
import io.cattle.platform.core.addon.LoadBalancerServiceLink;
import io.cattle.platform.core.addon.ServiceLink;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.LoadBalancerConstants;
import io.cattle.platform.core.model.Environment;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.ServiceConsumeMap;
import io.cattle.platform.docker.constants.DockerInstanceConstants;
import io.cattle.platform.docker.constants.DockerNetworkConstants;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.servicediscovery.api.constants.ServiceDiscoveryConstants;
import io.cattle.platform.servicediscovery.api.dao.ServiceConsumeMapDao;
import io.cattle.platform.servicediscovery.api.resource.ServiceDiscoveryConfigItem;
import io.cattle.platform.servicediscovery.api.service.RancherConfigToComposeFormatter;
import io.cattle.platform.servicediscovery.api.service.ServiceDiscoveryApiService;
import io.cattle.platform.servicediscovery.api.util.ServiceDiscoveryUtil;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.TransformerUtils;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.DumperOptions.LineBreak;
import org.yaml.snakeyaml.Yaml;

@Named
public class ServiceDiscoveryApiServiceImpl implements ServiceDiscoveryApiService {
    @Inject
    ObjectManager objectManager;

    @Inject
    ObjectProcessManager objectProcessManager;

    @Inject
    ServiceConsumeMapDao consumeMapDao;

    @Inject
    List<RancherConfigToComposeFormatter> formatters;

    @Inject
    AllocatorService allocatorService;


    @Override
    public void addServiceLink(Service service, ServiceLink serviceLink) {
        consumeMapDao.createServiceLink(service, serviceLink);
    }

    @Override
    public void removeServiceLink(Service service, ServiceLink serviceLink) {
        ServiceConsumeMap map = consumeMapDao.findMapToRemove(service.getId(), serviceLink.getServiceId());

        if (map != null) {
            objectProcessManager.scheduleProcessInstance(ServiceDiscoveryConstants.PROCESS_SERVICE_CONSUME_MAP_REMOVE,
                    map, null);
        }
    }

    @Override
    public void addLoadBalancerServiceLink(Service service, LoadBalancerServiceLink serviceLink) {
        if (!service.getKind().equalsIgnoreCase(ServiceDiscoveryConstants.KIND.LOADBALANCERSERVICE.name())) {
            return;
        }

        consumeMapDao.createServiceLink(service, serviceLink);
    }

    @Override
    public List<? extends Service> listEnvironmentServices(long environmentId) {
        return objectManager.find(Service.class, SERVICE.ENVIRONMENT_ID, environmentId, SERVICE.REMOVED,
                null);
    }

    @Override
    public Map.Entry<String, String> buildComposeConfig(List<? extends Service> services) {
        return new SimpleEntry<>(buildDockerComposeConfig(services), buildRancherComposeConfig(services));
    }

    @Override
    public String buildDockerComposeConfig(List<? extends Service> services) {
        Map<String, Object> dockerComposeData = createComposeData(services, true);
        return convertToYml(dockerComposeData);
    }

    @Override
    public String buildRancherComposeConfig(List<? extends Service> services) {
        Map<String, Object> dockerComposeData = createComposeData(services, false);
        return convertToYml(dockerComposeData);
    }

    private String convertToYml(Map<String, Object> dockerComposeData) {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setLineBreak(LineBreak.WIN);
        Yaml yaml = new Yaml(options);
        String yamlStr = yaml.dump(dockerComposeData);
        return yamlStr.replaceAll("[$]", "\\$\\$");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> createComposeData(List<? extends Service> servicesToExport, boolean forDockerCompose) {
        Map<String, Object> data = new HashMap<String, Object>();
        Collection<Long> servicesToExportIds = CollectionUtils.collect(servicesToExport,
                TransformerUtils.invokerTransformer("getId"));
        for (Service service : servicesToExport) {
            List<String> launchConfigNames = ServiceDiscoveryUtil.getServiceLaunchConfigNames(service);
            for (String launchConfigName : launchConfigNames) {
                boolean isPrimaryConfig = launchConfigName
                        .equals(ServiceDiscoveryConstants.PRIMARY_LAUNCH_CONFIG_NAME);
                Map<String, Object> cattleServiceData = ServiceDiscoveryUtil.getServiceDataAsMap(service,
                        launchConfigName, allocatorService);
                Map<String, Object> composeServiceData = new HashMap<>();
                formatScale(service, cattleServiceData);
                for (String cattleService : cattleServiceData.keySet()) {
                    translateRancherToCompose(forDockerCompose, cattleServiceData, composeServiceData, cattleService, service);
                }

                if (forDockerCompose) {
                    populateLinksForService(service, servicesToExportIds, composeServiceData);
                    populateNetworkForService(service, launchConfigName, composeServiceData);
                    populateVolumesForService(service, launchConfigName, composeServiceData);
                    addExtraComposeParameters(service, launchConfigName, composeServiceData);
                    populateSidekickLabels(service, composeServiceData, isPrimaryConfig);
                    populateLoadBalancerServiceLabels(service, launchConfigName, composeServiceData);
                    populateSelectorServiceLabels(service, launchConfigName, composeServiceData);
                }
                if (!composeServiceData.isEmpty()) {
                    data.put(isPrimaryConfig ? service.getName() : launchConfigName, composeServiceData);
                }
            }
        }
        return data;
    }
    
    @SuppressWarnings("unchecked")
    protected void formatScale(Service service, Map<String, Object> composeServiceData) {
        if (composeServiceData.get(InstanceConstants.FIELD_LABELS) != null) {
            Map<String, String> labels = ((HashMap<String, String>) composeServiceData.get(InstanceConstants.FIELD_LABELS));
            if (labels.containsKey(ServiceDiscoveryConstants.LABEL_SERVICE_GLOBAL)
                    && labels.get(ServiceDiscoveryConstants.LABEL_SERVICE_GLOBAL).equals(String.valueOf(Boolean.TRUE))) {
                composeServiceData.remove(ServiceDiscoveryConstants.FIELD_SCALE);
            }
        }
    }

    @SuppressWarnings("unchecked")
    protected void populateLoadBalancerServiceLabels(Service service,
            String launchConfigName, Map<String, Object> composeServiceData) {
        if (!service.getKind().equalsIgnoreCase(ServiceDiscoveryConstants.KIND.LOADBALANCERSERVICE.name())) {
            return;
        }

        Map<String, String> labels = new HashMap<>();
        if (composeServiceData.get(InstanceConstants.FIELD_LABELS) != null) {
            labels.putAll((HashMap<String, String>) composeServiceData.get(InstanceConstants.FIELD_LABELS));
        }
        // get all consumed services maps
        List<? extends ServiceConsumeMap> consumedServiceMaps = consumeMapDao.findConsumedServices(service.getId());
        // for each port, populate the label
        for (ServiceConsumeMap map : consumedServiceMaps) {
            Service consumedService = objectManager.loadResource(Service.class, map.getConsumedServiceId());
            List<String> ports = DataAccessor.fieldStringList(map, LoadBalancerConstants.FIELD_LB_TARGET_PORTS);
            String consumedServiceName = consumedService.getName();
            if (!service.getEnvironmentId().equals(consumedService.getEnvironmentId())) {
                Environment env = objectManager.loadResource(Environment.class,
                        consumedService.getEnvironmentId());
                consumedServiceName = env.getName() + "/" + consumedServiceName;
            }
            String labelName = ServiceDiscoveryConstants.LABEL_LB_TARGET + consumedServiceName;
            StringBuilder bldr = new StringBuilder();
            for (String port : ports) {
                bldr.append(port).append(",");
            }
            if (bldr.length() > 0) {
                labels.put(labelName, bldr.toString().substring(0, bldr.length() - 1));
            }
        }

        if (!labels.isEmpty()) {
            composeServiceData.put(InstanceConstants.FIELD_LABELS, labels);
        }
    }

    @SuppressWarnings("unchecked")
    protected void populateSelectorServiceLabels(Service service,
            String launchConfigName, Map<String, Object> composeServiceData) {
        String selectorContainer = service.getSelectorContainer();
        String selectorLink = service.getSelectorLink();
        if (selectorContainer == null && selectorLink == null) {
            return;
        }

        Map<String, String> labels = new HashMap<>();
        if (composeServiceData.get(InstanceConstants.FIELD_LABELS) != null) {
            labels.putAll((HashMap<String, String>) composeServiceData.get(InstanceConstants.FIELD_LABELS));
        }
        if (selectorLink != null) {
            labels.put(ServiceDiscoveryConstants.LABEL_SELECTOR_LINK, selectorLink);
        }
        if (selectorContainer != null) {
            labels.put(ServiceDiscoveryConstants.LABEL_SELECTOR_CONTAINER, selectorContainer);
        }

        if (!labels.isEmpty()) {
            composeServiceData.put(InstanceConstants.FIELD_LABELS, labels);
        }
    }

    @SuppressWarnings("unchecked")
    protected void populateSidekickLabels(Service service, Map<String, Object> composeServiceData, boolean isPrimary) {
        List<? extends String> configs = ServiceDiscoveryUtil
                .getServiceLaunchConfigNames(service);
        configs.remove(ServiceDiscoveryConstants.PRIMARY_LAUNCH_CONFIG_NAME);
        StringBuilder sidekicks = new StringBuilder();
        for (String config : configs) {
            sidekicks.append(config).append(",");
        }
        Map<String, String> labels = new HashMap<>();
        if (composeServiceData.get(InstanceConstants.FIELD_LABELS) != null) {
            labels.putAll((HashMap<String, String>) composeServiceData.get(InstanceConstants.FIELD_LABELS));
            labels.remove(ServiceDiscoveryConstants.LABEL_SIDEKICK);
        }
        if (!sidekicks.toString().isEmpty() && isPrimary) {
            String sidekicksFinal = sidekicks.toString().substring(0, sidekicks.length() - 1);
            labels.put(ServiceDiscoveryConstants.LABEL_SIDEKICK, sidekicksFinal);
        }

        if (!labels.isEmpty()) {
            composeServiceData.put(InstanceConstants.FIELD_LABELS, labels);
        } else {
            composeServiceData.remove(InstanceConstants.FIELD_LABELS);
        }
    }

    private void populateLinksForService(Service service, Collection<Long> servicesToExportIds,
            Map<String, Object> composeServiceData) {
        List<String> serviceLinksWithNames = new ArrayList<>();
        List<String> externalLinksServices = new ArrayList<>();
        List<? extends ServiceConsumeMap> consumedServiceMaps = consumeMapDao.findConsumedServices(service.getId());
        for (ServiceConsumeMap consumedServiceMap : consumedServiceMaps) {
            Service consumedService = objectManager.findOne(Service.class, SERVICE.ID,
                    consumedServiceMap.getConsumedServiceId());

            String linkName = consumedService.getName() + ":" + (consumedServiceMap.getName() != null ? consumedServiceMap.getName() : consumedService
                    .getName());
            if (servicesToExportIds.contains(consumedServiceMap.getConsumedServiceId())) {
                serviceLinksWithNames.add(linkName);
            } else if (!consumedService.getEnvironmentId().equals(service.getEnvironmentId())) {
                Environment externalEnvironment = objectManager.loadResource(Environment.class,
                        consumedService.getEnvironmentId());
                externalLinksServices.add(externalEnvironment.getName() + "/" + linkName);
            }
        }
        if (!serviceLinksWithNames.isEmpty()) {
            composeServiceData.put(ServiceDiscoveryConfigItem.LINKS.getDockerName(), serviceLinksWithNames);
        }

        if (!externalLinksServices.isEmpty()) {
            composeServiceData.put(ServiceDiscoveryConfigItem.EXTERNALLINKS.getDockerName(), externalLinksServices);
        }
    }

    private void addExtraComposeParameters(Service service,
            String launchConfigName, Map<String, Object> composeServiceData) {
        if (service.getKind().equalsIgnoreCase(ServiceDiscoveryConstants.KIND.DNSSERVICE.name())) {
            composeServiceData.put(ServiceDiscoveryConfigItem.IMAGE.getDockerName(), "rancher/dns-service");
        } else if (service.getKind().equalsIgnoreCase(ServiceDiscoveryConstants.KIND.LOADBALANCERSERVICE.name())) {
            composeServiceData.put(ServiceDiscoveryConfigItem.IMAGE.getDockerName(), "rancher/load-balancer-service");
        } else if (service.getKind().equalsIgnoreCase(ServiceDiscoveryConstants.KIND.EXTERNALSERVICE.name())) {
            composeServiceData.put(ServiceDiscoveryConfigItem.IMAGE.getDockerName(), "rancher/external-service");
        }
    }

    private void populateNetworkForService(Service service,
            String launchConfigName, Map<String, Object> composeServiceData) {
        Object networkMode = composeServiceData.get(ServiceDiscoveryConfigItem.NETWORKMODE.getDockerName());
        if (networkMode != null) {
            if (networkMode.equals(DockerNetworkConstants.NETWORK_MODE_CONTAINER)) {
                Map<String, Object> serviceData = ServiceDiscoveryUtil.getLaunchConfigDataAsMap(service,
                        launchConfigName);
                // network mode can be passed by container, or by service name, so check both
                // networkFromContainerId wins
                Integer targetContainerId = DataAccessor
                        .fieldInteger(service, DockerInstanceConstants.DOCKER_CONTAINER);
                if (targetContainerId != null) {
                    Instance instance = objectManager.loadResource(Instance.class, targetContainerId.longValue());
                    String instanceName = ServiceDiscoveryUtil.getInstanceName(instance);
                    composeServiceData.put(ServiceDiscoveryConfigItem.NETWORKMODE.getDockerName(),
                            DockerNetworkConstants.NETWORK_MODE_CONTAINER + ":" + instanceName);
                } else {
                    Object networkLaunchConfig = serviceData
                            .get(ServiceDiscoveryConstants.FIELD_NETWORK_LAUNCH_CONFIG);
                    if (networkLaunchConfig != null) {
                        composeServiceData.put(ServiceDiscoveryConfigItem.NETWORKMODE.getDockerName(),
                                DockerNetworkConstants.NETWORK_MODE_CONTAINER + ":" + networkLaunchConfig);
                    }
                }
            } else if (networkMode.equals(DockerNetworkConstants.NETWORK_MODE_MANAGED)) {
                composeServiceData.remove(ServiceDiscoveryConfigItem.NETWORKMODE.getDockerName());
            }
        }
    }

    protected void translateRancherToCompose(boolean forDockerCompose, Map<String, Object> rancherServiceData,
            Map<String, Object> composeServiceData, String cattleName, Service service) {
        ServiceDiscoveryConfigItem item = ServiceDiscoveryConfigItem.getServiceConfigItemByCattleName(cattleName, service);
        if (item != null && item.isDockerComposeProperty() == forDockerCompose) {
            Object value = rancherServiceData.get(cattleName);
            boolean export = false;
            if (value instanceof List) {
                if (!((List<?>) value).isEmpty()) {
                    export = true;
                }
            } else if (value instanceof Map) {
                if (!((Map<?, ?>) value).isEmpty()) {
                    export = true;
                }
            } else if (value instanceof Boolean) {
                if (((Boolean) value).booleanValue()) {
                    export = true;
                }
            } else if (value != null) {
                export = true;
            }
            if (export) {
                // for every lookup, do transform
                Object formattedValue = null;
                for (RancherConfigToComposeFormatter formatter : formatters) {
                    formattedValue = formatter.format(item, value);
                    if (formattedValue != null) {
                        break;
                    }
                }
                if (formattedValue != null) {
                    if (formattedValue != RancherConfigToComposeFormatter.Option.REMOVE) {
                        composeServiceData.put(item.getDockerName().toLowerCase(), formattedValue);
                    }
                } else {
                    composeServiceData.put(item.getDockerName().toLowerCase(), value);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void populateVolumesForService(Service service, String launchConfigName,
            Map<String, Object> composeServiceData) {
        List<String> namesCombined = new ArrayList<>();
        List<String> launchConfigNames = new ArrayList<>();
        Map<String, Object> launchConfigData = ServiceDiscoveryUtil.getLaunchConfigDataAsMap(service, launchConfigName);
        Object dataVolumesLaunchConfigs = launchConfigData.get(
                ServiceDiscoveryConstants.FIELD_DATA_VOLUMES_LAUNCH_CONFIG);

        if (dataVolumesLaunchConfigs != null) {
            launchConfigNames.addAll((List<String>) dataVolumesLaunchConfigs);
        }

        // 1. add launch config names
        namesCombined.addAll(launchConfigNames);

        // 2. add instance names if specified
        List<? extends Integer> instanceIds = (List<? extends Integer>) launchConfigData
                .get(DockerInstanceConstants.FIELD_VOLUMES_FROM);

        if (instanceIds != null) {
            for (Integer instanceId : instanceIds) {
                Instance instance = objectManager.findOne(Instance.class, INSTANCE.ID, instanceId, INSTANCE.REMOVED,
                        null);
                String instanceName = ServiceDiscoveryUtil.getInstanceName(instance);
                if (instanceName != null) {
                    namesCombined.add(instanceName);
                }
            }
        }

        if (!namesCombined.isEmpty()) {
            composeServiceData.put(ServiceDiscoveryConfigItem.VOLUMESFROM.getDockerName(), namesCombined);
        }
    }
    
}
