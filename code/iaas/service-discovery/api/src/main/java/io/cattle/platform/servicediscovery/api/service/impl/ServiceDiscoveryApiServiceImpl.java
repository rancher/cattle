package io.cattle.platform.servicediscovery.api.service.impl;

import static io.cattle.platform.core.model.tables.InstanceTable.*;
import static io.cattle.platform.core.model.tables.ServiceTable.*;
import static io.cattle.platform.core.model.tables.VolumeTemplateTable.*;
import io.cattle.platform.core.addon.LoadBalancerServiceLink;
import io.cattle.platform.core.addon.ServiceLink;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.LoadBalancerConstants;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.dao.DataDao;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.ServiceConsumeMap;
import io.cattle.platform.core.model.Stack;
import io.cattle.platform.core.model.VolumeTemplate;
import io.cattle.platform.docker.constants.DockerInstanceConstants;
import io.cattle.platform.docker.constants.DockerNetworkConstants;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.object.util.DataUtils;
import io.cattle.platform.servicediscovery.api.dao.ServiceConsumeMapDao;
import io.cattle.platform.servicediscovery.api.resource.ServiceDiscoveryConfigItem;
import io.cattle.platform.servicediscovery.api.service.RancherConfigToComposeFormatter;
import io.cattle.platform.servicediscovery.api.service.ServiceDiscoveryApiService;
import io.cattle.platform.servicediscovery.api.util.ServiceDiscoveryUtil;
import io.cattle.platform.token.CertSet;
import io.cattle.platform.token.impl.RSAKeyProvider;

import java.io.ByteArrayOutputStream;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.codec.binary.Base64;
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
    JsonMapper jsonMapper;

    @Inject
    RSAKeyProvider keyProvider;

    @Inject
    DataDao dataDao;

    @Override
    public void addServiceLink(Service service, ServiceLink serviceLink) {
        consumeMapDao.createServiceLink(service, serviceLink);
    }

    @Override
    public void removeServiceLink(Service service, ServiceLink serviceLink) {
        ServiceConsumeMap map = consumeMapDao.findMapToRemove(service.getId(), serviceLink.getServiceId());

        if (map != null) {
            objectProcessManager.scheduleProcessInstance(ServiceConstants.PROCESS_SERVICE_CONSUME_MAP_REMOVE,
                    map, null);
        }
    }

    @Override
    public void addLoadBalancerServiceLink(Service service, LoadBalancerServiceLink serviceLink) {
        if (!service.getKind().equalsIgnoreCase(ServiceConstants.KIND_LOAD_BALANCER_SERVICE)) {
            return;
        }

        consumeMapDao.createServiceLink(service, serviceLink);
    }

    @Override
    public List<? extends Service> listStackServices(long stackId) {
        return objectManager.find(Service.class, SERVICE.STACK_ID, stackId, SERVICE.REMOVED,
                null);
    }

    @Override
    public Map.Entry<String, String> buildComposeConfig(List<? extends Service> services, Stack stack) {
        return new SimpleEntry<>(buildDockerComposeConfig(services, stack), buildRancherComposeConfig(services));
    }

    @Override
    public String buildDockerComposeConfig(List<? extends Service> services, Stack stack) {
        List<? extends VolumeTemplate> volumes = objectManager.find(VolumeTemplate.class, VOLUME_TEMPLATE.STACK_ID,
                stack.getId(),
                    VOLUME_TEMPLATE.REMOVED, null);
                
        Map<String, Object> dockerComposeData = createComposeData(services, true, volumes);
        return "version: '2'\n" + convertToYml(dockerComposeData);
    }

    @Override
    public String buildRancherComposeConfig(List<? extends Service> services) {
        Map<String, Object> dockerComposeData = createComposeData(services, false, new ArrayList<VolumeTemplate>());
        return "version: '2'\n" + convertToYml(dockerComposeData);
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
    private Map<String, Object> createComposeData(List<? extends Service> servicesToExport, boolean forDockerCompose, List<? extends VolumeTemplate> volumes) {
        Map<String, Object> servicesData = new HashMap<String, Object>();
        Collection<Long> servicesToExportIds = CollectionUtils.collect(servicesToExport,
                TransformerUtils.invokerTransformer("getId"));
        for (Service service : servicesToExport) {
            List<String> launchConfigNames = ServiceDiscoveryUtil.getServiceLaunchConfigNames(service);
            for (String launchConfigName : launchConfigNames) {
                boolean isPrimaryConfig = launchConfigName
                        .equals(ServiceConstants.PRIMARY_LAUNCH_CONFIG_NAME);
                Map<String, Object> cattleServiceData = ServiceDiscoveryUtil.getLaunchConfigWithServiceDataAsMap(
                        service, launchConfigName);
                Map<String, Object> composeServiceData = new HashMap<>();
                excludeRancherHash(cattleServiceData);
                formatScale(service, cattleServiceData);
                setupServiceType(service, cattleServiceData);
                for (String cattleService : cattleServiceData.keySet()) {
                    translateRancherToCompose(forDockerCompose, cattleServiceData, composeServiceData, cattleService, service, false);
                }

                if (forDockerCompose) {
                    populateLinksForService(service, servicesToExportIds, composeServiceData);
                    populateNetworkForService(service, launchConfigName, composeServiceData);
                    populateVolumesForService(service, launchConfigName, composeServiceData);
                    addExtraComposeParameters(service, launchConfigName, composeServiceData);
                    populateSidekickLabels(service, composeServiceData, isPrimaryConfig);
                    populateLoadBalancerServiceLabels(service, launchConfigName, composeServiceData);
                    populateSelectorServiceLabels(service, launchConfigName, composeServiceData);
                    populateLogConfig(cattleServiceData, composeServiceData);
                }
                if (!composeServiceData.isEmpty()) {
                    servicesData.put(isPrimaryConfig ? service.getName() : launchConfigName, composeServiceData);
                }
            }
        }

        Map<String, Object> volumesData = new HashMap<String, Object>();
        for (VolumeTemplate volume : volumes) {
            Map<String, Object> cattleVolumeData = new HashMap<>();
            cattleVolumeData.putAll(DataUtils.getFields(volume));
            cattleVolumeData.put(ServiceConstants.FIELD_VOLUME_EXTERNAL, volume.getExternal());
            cattleVolumeData.put(ServiceConstants.FIELD_VOLUME_DRIVER, volume.getDriver());
            cattleVolumeData.put(ServiceConstants.FIELD_VOLUME_PER_CONTAINER, volume.getPerContainer());
            Map<String, Object> composeVolumeData = new HashMap<>();
            for (String cattleVolume : cattleVolumeData.keySet()) {
                translateRancherToCompose(forDockerCompose, cattleVolumeData, composeVolumeData, cattleVolume,
                        null, true);
            }
            if (!composeVolumeData.isEmpty()) {
                volumesData.put(volume.getName(), composeVolumeData);
            }
        }

        Map<String, Object> data = new HashMap<String, Object>();
        if (!servicesData.isEmpty()) {
            data.put("services", servicesData);
        }
        if (!volumesData.isEmpty()) {
            data.put("volumes", volumesData);
        }
        return data;
    }

    @SuppressWarnings("unchecked")
    private void excludeRancherHash(Map<String, Object> composeServiceData) {
        if (composeServiceData.get(InstanceConstants.FIELD_LABELS) != null) {
            Map<String, String> labels = new HashMap<>();
            labels.putAll((HashMap<String, String>) composeServiceData.get(InstanceConstants.FIELD_LABELS));
            String serviceHash = labels.get(ServiceConstants.LABEL_SERVICE_HASH);
            if (serviceHash != null) {
                labels.remove(ServiceConstants.LABEL_SERVICE_HASH);
                composeServiceData.put(InstanceConstants.FIELD_LABELS, labels);
            }
        }

        if (composeServiceData.get(InstanceConstants.FIELD_METADATA) != null) {
            Map<String, String> metadata = new HashMap<>();
            metadata.putAll((HashMap<String, String>) composeServiceData.get(InstanceConstants.FIELD_METADATA));
            String serviceHash = metadata.get(ServiceConstants.LABEL_SERVICE_HASH);
            if (serviceHash != null) {
                metadata.remove(ServiceConstants.LABEL_SERVICE_HASH);
                composeServiceData.put(InstanceConstants.FIELD_METADATA, metadata);
            }
        }

    }

    @SuppressWarnings("unchecked")
    protected void formatScale(Service service, Map<String, Object> composeServiceData) {
        if (composeServiceData.get(InstanceConstants.FIELD_LABELS) != null) {
            Map<String, String> labels = ((HashMap<String, String>) composeServiceData.get(InstanceConstants.FIELD_LABELS));
            String globalService = labels.get(ServiceConstants.LABEL_SERVICE_GLOBAL);
            if (Boolean.valueOf(globalService) == true) {
                composeServiceData.remove(ServiceConstants.FIELD_SCALE);
            }
        }
    }

    protected void setupServiceType(Service service, Map<String, Object> composeServiceData) {
        Object type = composeServiceData.get(ServiceDiscoveryConfigItem.SERVICE_TYPE.getCattleName());
        if (type == null) {
            return;
        }
        if (!InstanceConstants.KIND_VIRTUAL_MACHINE.equals(type.toString())) {
            composeServiceData.remove(ServiceDiscoveryConfigItem.SERVICE_TYPE.getCattleName());
        }
    }

    @SuppressWarnings("unchecked")
    private void populateLogConfig(Map<String, Object> cattleServiceData, Map<String, Object> composeServiceData) {
        Object value = cattleServiceData.get(ServiceConstants.FIELD_LOG_CONFIG);
        if (value instanceof Map) {
            if (!((Map<?, ?>) value).isEmpty()) {
                Map<String, Object> map = (Map<String, Object>)value;
                Iterator<String> it = map.keySet().iterator();
                while (it.hasNext()) {
                    String key = it.next();
                    if (key.equalsIgnoreCase("config") && map.get(key) != null) {
                        if (map.get(key) instanceof java.util.Map && !((Map<?, ?>)map.get(key)).isEmpty())
                            composeServiceData.put("log_opt", map.get(key));
                    } else if (key.equalsIgnoreCase("driver") && map.get(key) != null && map.get(key) != "") {
                        composeServiceData.put("log_driver", map.get(key));
                    }
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    protected void populateLoadBalancerServiceLabels(Service service,
            String launchConfigName, Map<String, Object> composeServiceData) {
        if (!service.getKind().equalsIgnoreCase(ServiceConstants.KIND_LOAD_BALANCER_SERVICE)) {
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
            if (!service.getStackId().equals(consumedService.getStackId())) {
                Stack env = objectManager.loadResource(Stack.class,
                        consumedService.getStackId());
                consumedServiceName = env.getName() + "/" + consumedServiceName;
            }
            String labelName = ServiceConstants.LABEL_LB_TARGET + consumedServiceName;
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
            labels.put(ServiceConstants.LABEL_SELECTOR_LINK, selectorLink);
        }
        if (selectorContainer != null) {
            labels.put(ServiceConstants.LABEL_SELECTOR_CONTAINER, selectorContainer);
        }

        if (!labels.isEmpty()) {
            composeServiceData.put(InstanceConstants.FIELD_LABELS, labels);
        }
    }

    @SuppressWarnings("unchecked")
    protected void populateSidekickLabels(Service service, Map<String, Object> composeServiceData, boolean isPrimary) {
        List<? extends String> configs = ServiceDiscoveryUtil
                .getServiceLaunchConfigNames(service);
        configs.remove(ServiceConstants.PRIMARY_LAUNCH_CONFIG_NAME);
        StringBuilder sidekicks = new StringBuilder();
        for (String config : configs) {
            sidekicks.append(config).append(",");
        }
        Map<String, String> labels = new HashMap<>();
        if (composeServiceData.get(InstanceConstants.FIELD_LABELS) != null) {
            labels.putAll((HashMap<String, String>) composeServiceData.get(InstanceConstants.FIELD_LABELS));
            labels.remove(ServiceConstants.LABEL_SIDEKICK);
        }
        if (!sidekicks.toString().isEmpty() && isPrimary) {
            String sidekicksFinal = sidekicks.toString().substring(0, sidekicks.length() - 1);
            labels.put(ServiceConstants.LABEL_SIDEKICK, sidekicksFinal);
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
            } else if (!consumedService.getStackId().equals(service.getStackId())) {
                Stack externalStack = objectManager.loadResource(Stack.class,
                        consumedService.getStackId());
                externalLinksServices.add(externalStack.getName() + "/" + linkName);
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
        if (service.getKind().equalsIgnoreCase(ServiceConstants.KIND_DNS_SERVICE)) {
            composeServiceData.put(ServiceDiscoveryConfigItem.IMAGE.getDockerName(), "rancher/dns-service");
        } else if (service.getKind().equalsIgnoreCase(ServiceConstants.KIND_LOAD_BALANCER_SERVICE)) {
            composeServiceData.put(ServiceDiscoveryConfigItem.IMAGE.getDockerName(), "rancher/load-balancer-service");
        } else if (service.getKind().equalsIgnoreCase(ServiceConstants.KIND_EXTERNAL_SERVICE)) {
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
                            .get(ServiceConstants.FIELD_NETWORK_LAUNCH_CONFIG);
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
            Map<String, Object> composeServiceData, String cattleName, Service service, boolean isVolume) {
        ServiceDiscoveryConfigItem item = ServiceDiscoveryConfigItem.getServiceConfigItemByCattleName(cattleName,
                service, isVolume);
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
                ServiceConstants.FIELD_DATA_VOLUMES_LAUNCH_CONFIG);

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

    @Override
    public String getServiceCertificate(final Service service) {
        if (service == null) {
            return null;
        }

        final Stack stack = objectManager.loadResource(Stack.class, service.getStackId());

        if (stack == null) {
            return null;
        }

        String key = String.format("service.%d.cert", service.getId());

        return dataDao.getOrCreate(key, false, new Callable<String>() {
            @Override
            public String call() throws Exception {
                return generateService(service, stack);
            }
        });
    }

    protected String generateService(Service service, Stack stack) throws Exception {
        @SuppressWarnings("unchecked")
        Map<String, Object> metadata = DataAccessor.fields(service).withKey(ServiceConstants.FIELD_METADATA)
                .withDefault(Collections.EMPTY_MAP).as(Map.class);

        String serviceName = service.getName();
        List<? extends String> configuredSans = DataAccessor.fromMap(metadata).withKey("sans")
            .withDefault(Collections.emptyList()).asList(jsonMapper, String.class);
        List<String> sans = new ArrayList<>(configuredSans);

        sans.add(serviceName.toLowerCase());
        sans.add(String.format("%s.%s", serviceName, stack.getName()).toLowerCase());


        CertSet certSet = keyProvider.generateCertificate(serviceName, sans.toArray(new String[sans.size()]));
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        certSet.writeZip(baos);

        return Base64.encodeBase64String(baos.toByteArray());
    }
}