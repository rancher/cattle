package io.cattle.platform.servicediscovery.api.export.impl;

import static io.cattle.platform.core.model.tables.InstanceTable.*;
import static io.cattle.platform.core.model.tables.ServiceTable.*;
import static io.cattle.platform.core.model.tables.VolumeTemplateTable.*;
import io.cattle.platform.core.addon.LbConfig;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.LoadBalancerConstants;
import io.cattle.platform.core.constants.NetworkConstants;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.dao.LoadBalancerInfoDao;
import io.cattle.platform.core.dao.ServiceConsumeMapDao;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.ServiceConsumeMap;
import io.cattle.platform.core.model.Stack;
import io.cattle.platform.core.model.VolumeTemplate;
import io.cattle.platform.core.util.LBMetadataUtil.LBConfigMetadataStyle;
import io.cattle.platform.core.util.ServiceUtil;
import io.cattle.platform.docker.constants.DockerInstanceConstants;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.object.util.DataUtils;
import io.cattle.platform.servicediscovery.api.export.ServiceDiscoveryComposeExportService;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.TransformerUtils;
import org.apache.commons.lang3.StringUtils;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.DumperOptions.LineBreak;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.introspector.Property;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;

@Named
public class ServiceDiscoveryComposeExportServiceImpl implements ServiceDiscoveryComposeExportService {
    @Inject
    ObjectManager objectManager;
    @Inject
    ServiceConsumeMapDao consumeMapDao;
    @Inject
    List<RancherConfigToComposeFormatter> formatters;
    @Inject
    JsonMapper jsonMapper;
    @Inject
    LoadBalancerInfoDao lbInfoDao;

    private final static String COMPOSE_PREFIX = "version: '2'\r\n";

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
        if (dockerComposeData.isEmpty()) {
            return COMPOSE_PREFIX;
        } else {
            return COMPOSE_PREFIX + convertToYml(dockerComposeData);
        }
    }

    @Override
    public String buildRancherComposeConfig(List<? extends Service> services) {
        Map<String, Object> dockerComposeData = createComposeData(services, false, new ArrayList<VolumeTemplate>());
        if (dockerComposeData.isEmpty()) {
            return COMPOSE_PREFIX;
        } else {
            return COMPOSE_PREFIX + convertToYml(dockerComposeData);
        }
    }

    private String convertToYml(Map<String, Object> dockerComposeData) {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setLineBreak(LineBreak.WIN);
        Representer representer = new SkipNullRepresenter();
        representer.addClassTag(LBConfigMetadataStyle.class, Tag.MAP);

        Yaml yaml = new Yaml(representer, options);
        String yamlStr = yaml.dump(dockerComposeData);
        return yamlStr.replaceAll("[$]", "\\$\\$");
    }

    private class SkipNullRepresenter extends Representer {
        @Override
        protected NodeTuple representJavaBeanProperty(Object javaBean, Property property,
                Object propertyValue, Tag customTag) {
            if (propertyValue == null) {
                return null;
            } else {
                return super
                        .representJavaBeanProperty(javaBean, property, propertyValue, customTag);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> createComposeData(List<? extends Service> servicesToExport, boolean forDockerCompose,
            List<? extends VolumeTemplate> volumes) {
        Map<String, Object> servicesData = new HashMap<String, Object>();
        Collection<Long> servicesToExportIds = CollectionUtils.collect(servicesToExport,
                TransformerUtils.invokerTransformer("getId"));
        Map<String, Object> volumesData = new HashMap<String, Object>();
        for (Service service : servicesToExport) {
            List<String> launchConfigNames = ServiceUtil.getLaunchConfigNames(service);
            for (String launchConfigName : launchConfigNames) {
                boolean isPrimaryConfig = launchConfigName
                        .equals(ServiceConstants.PRIMARY_LAUNCH_CONFIG_NAME);
                Map<String, Object> cattleServiceData = ServiceUtil.getLaunchConfigWithServiceDataAsMap(
                        service, launchConfigName);
                Map<String, Object> composeServiceData = new HashMap<>();
                excludeRancherHash(cattleServiceData);
                formatScale(service, cattleServiceData);
                formatLBConfig(service, cattleServiceData);
                setupServiceType(service, cattleServiceData);
                for (String cattleService : cattleServiceData.keySet()) {
                    translateRancherToCompose(forDockerCompose, cattleServiceData, composeServiceData, cattleService,
                            service, false);
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
                    populateTmpfs(cattleServiceData, composeServiceData);
                    populateUlimit(cattleServiceData, composeServiceData);
                    populateBlkioOptions(cattleServiceData, composeServiceData);
                    translateV1VolumesToV2(cattleServiceData, composeServiceData, volumesData);
                }
                if (!composeServiceData.isEmpty()) {
                    servicesData.put(isPrimaryConfig ? service.getName() : launchConfigName, composeServiceData);
                }
            }
        }

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
    private void populateTmpfs(Map<String, Object> cattleServiceData, Map<String, Object> composeServiceData) {
        Object value = cattleServiceData.get(ServiceConstants.FIELD_TMPFS);
        if (value instanceof Map) {
            if (!((Map<?, ?>) value).isEmpty()) {
                Map<String, Object> map = (Map<String, Object>) value;
                Iterator<String> it = map.keySet().iterator();
                ArrayList<String> list = new ArrayList<>();
                while (it.hasNext()) {
                    String key = it.next();
                    String option = "";
                    if (map.get(key) != null) {
                        option = map.get(key).toString();
                    }
                    if (option.isEmpty()) {
                        list.add(key);
                    } else {
                        list.add(key + ":" + option);
                    }
                }
                composeServiceData.put("tmpfs", list);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void populateUlimit(Map<String, Object> cattleServiceData, Map<String, Object> composeServiceData) {
        Object value = cattleServiceData.get(ServiceConstants.FIELD_ULIMITS);
        if (value instanceof List<?>) {
            if (!((List<?>) value).isEmpty()) {
                List<Object> list = (List<Object>) value;
                Map<String, Object> ulimits = new HashMap<>();
                for (Object ulimit : list) {
                    // if there is one limit set(must be soft), parse it as map[string]string. If not, parse it as
// nested map
                    if (ulimit instanceof Map) {
                        Map<String, Object> ulimitMap = (Map<String, Object>) ulimit;
                        // name can not be null
                        if (ulimitMap.get("name").toString() != null) {
                            if (ulimitMap.get("soft") != null && ulimitMap.get("hard") == null) {
                                ulimits.put(ulimitMap.get("name").toString(), ulimitMap.get("soft"));
                            }
                            else if (ulimitMap.get("soft") != null && ulimitMap.get("hard") != null) {
                                Map<String, Object> nestedMap = new HashMap<>();
                                nestedMap.put("hard", ulimitMap.get("hard"));
                                nestedMap.put("soft", ulimitMap.get("soft"));
                                ulimits.put(ulimitMap.get("name").toString(), nestedMap);
                            }
                        }
                    }

                }
                composeServiceData.put("ulimits", ulimits);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void populateBlkioOptions(Map<String, Object> cattleServiceData, Map<String, Object> composeServiceData) {
        Object value = cattleServiceData.get(ServiceConstants.FIELD_BLKIOOPTIONS);
        if (value instanceof Map) {
            if (!((Map<?, ?>) value).isEmpty()) {
                Map<String, Object> options = (Map<String, Object>) value;
                Map<String, Object> deviceWeight = new HashMap<>();
                Map<String, Object> deviceReadBps = new HashMap<>();
                Map<String, Object> deviceReadIops = new HashMap<>();
                Map<String, Object> deviceWriteBps = new HashMap<>();
                Map<String, Object> deviceWriteIops = new HashMap<>();
                for (String key : options.keySet()) {
                    Object option = options.get(key);
                    if (option instanceof Map) {
                        Map<String, Object> optionMap = (Map<String, Object>) option;
                        if (optionMap.get("readIops") != null) {
                            deviceReadIops.put(key, optionMap.get("readIops"));
                        }
                        if (optionMap.get("writeIops") != null) {
                            deviceWriteIops.put(key, optionMap.get("writeIops"));
                        }
                        if (optionMap.get("readBps") != null) {
                            deviceReadBps.put(key, optionMap.get("readBps"));
                        }
                        if (optionMap.get("writeBps") != null) {
                            deviceWriteBps.put(key, optionMap.get("writeBps"));
                        }
                        if (optionMap.get("weight") != null) {
                            deviceWeight.put(key, optionMap.get("weight"));
                        }
                    }
                }
                if (!deviceWeight.isEmpty()) {
                    composeServiceData.put("blkio_weight_device", deviceWeight);
                }
                if (!deviceReadBps.isEmpty()) {
                    composeServiceData.put("device_read_bps", deviceReadBps);
                }
                if (!deviceReadIops.isEmpty()) {
                    composeServiceData.put("device_read_iops", deviceReadIops);
                }
                if (!deviceWriteBps.isEmpty()) {
                    composeServiceData.put("device_write_bps", deviceWriteBps);
                }
                if (!deviceWriteIops.isEmpty()) {
                    composeServiceData.put("device_write_iops", deviceWriteIops);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    protected void populateLoadBalancerServiceLabels(Service service,
            String launchConfigName, Map<String, Object> composeServiceData) {
        // to support lb V1 export format
        if (!ServiceUtil.isV1LB(service)) {
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
        List<? extends String> configs = ServiceUtil
                .getLaunchConfigNames(service);
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
        // no export for lb service links for lb v2
        if (service.getKind().equalsIgnoreCase(ServiceConstants.KIND_LOAD_BALANCER_SERVICE)
                && !ServiceUtil.isV1LB(service)) {
            return;
        }
        List<String> serviceLinksWithNames = new ArrayList<>();
        List<String> externalLinksServices = new ArrayList<>();
        List<? extends ServiceConsumeMap> consumedServiceMaps = consumeMapDao.findConsumedServices(service.getId());
        for (ServiceConsumeMap consumedServiceMap : consumedServiceMaps) {
            Service consumedService = objectManager.findOne(Service.class, SERVICE.ID,
                    consumedServiceMap.getConsumedServiceId());

            String linkName = consumedService.getName()
                    + ":"
                    + (!StringUtils.isEmpty(consumedServiceMap.getName()) ? consumedServiceMap.getName()
                            : consumedService
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
        } else if (ServiceUtil.isV1LB(service)) {
            composeServiceData.put(ServiceDiscoveryConfigItem.IMAGE.getDockerName(),
                    "rancher/load-balancer-service");
        } else if (service.getKind().equalsIgnoreCase(ServiceConstants.KIND_EXTERNAL_SERVICE)) {
            composeServiceData.put(ServiceDiscoveryConfigItem.IMAGE.getDockerName(), "rancher/external-service");
        }
    }

    private void populateNetworkForService(Service service,
            String launchConfigName, Map<String, Object> composeServiceData) {
        Object networkMode = composeServiceData.get(ServiceDiscoveryConfigItem.NETWORKMODE.getDockerName());
        if (networkMode != null) {
            if (networkMode.equals(NetworkConstants.NETWORK_MODE_CONTAINER)) {
                Map<String, Object> serviceData = ServiceUtil.getLaunchConfigDataAsMap(service,
                        launchConfigName);
                // network mode can be passed by container, or by service name, so check both
                // networkFromContainerId wins
                Integer targetContainerId = DataAccessor
                        .fieldInteger(service, DockerInstanceConstants.DOCKER_CONTAINER);
                if (targetContainerId != null) {
                    Instance instance = objectManager.loadResource(Instance.class, targetContainerId.longValue());
                    String instanceName = ServiceUtil.getInstanceName(instance);
                    composeServiceData.put(ServiceDiscoveryConfigItem.NETWORKMODE.getDockerName(),
                            NetworkConstants.NETWORK_MODE_CONTAINER + ":" + instanceName);
                } else {
                    Object networkLaunchConfig = serviceData
                            .get(ServiceConstants.FIELD_NETWORK_LAUNCH_CONFIG);
                    if (networkLaunchConfig != null) {
                        composeServiceData.put(ServiceDiscoveryConfigItem.NETWORKMODE.getDockerName(),
                                NetworkConstants.NETWORK_MODE_CONTAINER + ":" + networkLaunchConfig);
                    }
                }
            } else if (networkMode.equals(NetworkConstants.NETWORK_MODE_MANAGED)) {
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
        Map<String, Object> launchConfigData = ServiceUtil.getLaunchConfigDataAsMap(service, launchConfigName);
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
                String instanceName = ServiceUtil.getInstanceName(instance);
                if (instanceName != null) {
                    namesCombined.add(instanceName);
                }
            }
        }

        if (!namesCombined.isEmpty()) {
            composeServiceData.put(ServiceDiscoveryConfigItem.VOLUMESFROM.getDockerName(), namesCombined);
        }
    }

    @SuppressWarnings("unchecked")
    private void translateV1VolumesToV2(Map<String, Object> cattleServiceData,
            Map<String, Object> composeServiceData, Map<String, Object> volumesData) {
        // volume driver presence defines the v1 format for the volumes
        String volumeDriver = String.valueOf(cattleServiceData.get(ServiceDiscoveryConfigItem.VOLUME_DRIVER
                .getCattleName()));
        if (StringUtils.isEmpty(volumeDriver)) {
            return;
        }
        composeServiceData.remove(ServiceDiscoveryConfigItem.VOLUME_DRIVER
                .getDockerName());
        Object dataVolumes = cattleServiceData.get(InstanceConstants.FIELD_DATA_VOLUMES);
        if (dataVolumes == null) {
            return;
        }

        for (String dataVolume : (List<String>) dataVolumes) {
            String[] splitted = dataVolume.split(":");
            if (splitted.length < 2) {
                // only process named volumes
                continue;
            }
            String dataVolumeName = splitted[0];
            // skip bind mounts
            Path p = Paths.get(dataVolumeName);
            if (p.isAbsolute()) {
                continue;
            }
            if (volumesData.containsKey(dataVolumeName)) {
                // either defined by volumeTemplate, or external volume is already created for it
                continue;
            }
            Map<String, Object> cattleVolumeData = new HashMap<>();
            cattleVolumeData.put(ServiceConstants.FIELD_VOLUME_EXTERNAL, true);
            cattleVolumeData.put(ServiceConstants.FIELD_VOLUME_DRIVER, volumeDriver);
            Map<String, Object> composeVolumeData = new HashMap<>();
            for (String cattleVolume : cattleVolumeData.keySet()) {
                translateRancherToCompose(true, cattleVolumeData, composeVolumeData, cattleVolume,
                        null, true);
            }
            if (!composeVolumeData.isEmpty()) {
                volumesData.put(dataVolumeName, composeVolumeData);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void populateLogConfig(Map<String, Object> cattleServiceData, Map<String, Object> composeServiceData) {
        Object value = cattleServiceData.get(ServiceConstants.FIELD_LOG_CONFIG);
        if (value instanceof Map) {
            if (!((Map<?, ?>) value).isEmpty()) {
                Map<String, Object> logConfig = new HashMap<>();
                Map<String, Object> map = (Map<String, Object>) value;
                Iterator<String> it = map.keySet().iterator();
                while (it.hasNext()) {
                    String key = it.next();
                    if (key.equalsIgnoreCase("config") && map.get(key) != null) {
                        if (map.get(key) instanceof java.util.Map && !((Map<?, ?>) map.get(key)).isEmpty()) {
                            logConfig.put("options", map.get(key));
                        }
                    } else if (key.equalsIgnoreCase("driver") && map.get(key) != null && map.get(key) != "") {
                        logConfig.put("driver", map.get(key));
                    }
                }
                if (!logConfig.isEmpty() && logConfig.get("driver") != null) {
                    composeServiceData.put("logging", logConfig);
                }
            }
        }
    }

    protected void formatLBConfig(Service service, Map<String, Object> composeServiceData) {
        if (composeServiceData.get(ServiceConstants.FIELD_LB_CONFIG) != null) {
            LbConfig lbConfig = DataAccessor.field(service, ServiceConstants.FIELD_LB_CONFIG, jsonMapper,
                    LbConfig.class);
            Stack selfStack = objectManager.loadResource(Stack.class, service.getStackId());
            composeServiceData.put(ServiceConstants.FIELD_LB_CONFIG,
                    new LBConfigMetadataStyle(lbConfig.getPortRules(), lbConfig.getCertificateIds(),
                            lbConfig.getDefaultCertificateId(),
                            lbConfig.getConfig(), lbConfig.getStickinessPolicy(), lbInfoDao
                                    .getServiceIdToServiceStackName(service.getAccountId()), lbInfoDao
                                    .getCertificateIdToCertificate(service.getAccountId()), selfStack.getName(), true,
                            lbInfoDao.getInstanceIdToInstanceName(service
                                    .getAccountId())));
        }
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
            Map<String, String> labels = ((HashMap<String, String>) composeServiceData
                    .get(InstanceConstants.FIELD_LABELS));
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
}
