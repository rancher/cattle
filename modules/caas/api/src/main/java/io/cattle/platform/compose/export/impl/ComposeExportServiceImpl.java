package io.cattle.platform.compose.export.impl;

import io.cattle.platform.compose.export.ComposeExportService;
import io.cattle.platform.core.addon.LbConfig;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.NetworkConstants;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.dao.LoadBalancerInfoDao;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.Stack;
import io.cattle.platform.core.model.VolumeTemplate;
import io.cattle.platform.core.util.LBMetadataUtil.LBConfigMetadataStyle;
import io.cattle.platform.core.util.ServiceUtil;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.DataAccessor;
import org.apache.commons.lang3.StringUtils;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.DumperOptions.LineBreak;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.introspector.Property;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static io.cattle.platform.core.model.tables.InstanceTable.*;
import static io.cattle.platform.core.model.tables.ServiceTable.*;
import static io.cattle.platform.core.model.tables.VolumeTemplateTable.*;
import static java.util.stream.Collectors.*;

public class ComposeExportServiceImpl implements ComposeExportService {

    private final static String COMPOSE_PREFIX = "version: '2'\r\n";

    ObjectManager objectManager;
    List<RancherConfigToComposeFormatter> formatters;
    LoadBalancerInfoDao lbInfoDao;

    public ComposeExportServiceImpl(ObjectManager objectManager, LoadBalancerInfoDao lbInfoDao, List<RancherConfigToComposeFormatter> formatters) {
        super();
        this.objectManager = objectManager;
        this.formatters = formatters;
        this.lbInfoDao = lbInfoDao;
    }

    @Override
    public Map.Entry<String, String> buildComposeConfig(List<? extends Service> services, Stack stack, boolean combined) {
        return new SimpleEntry<>(buildDockerComposeConfig(services, stack, combined), buildRancherComposeConfig(services, combined));
    }

    @Override
    public String buildDockerComposeConfig(List<? extends Service> services, Stack stack, boolean combined) {
        List<? extends VolumeTemplate> volumes = objectManager.find(VolumeTemplate.class, VOLUME_TEMPLATE.STACK_ID,
                stack.getId(),
                VOLUME_TEMPLATE.REMOVED, null);

        Map<String, Object> dockerComposeData = createComposeData(services, true, volumes, combined);
        if (dockerComposeData.isEmpty()) {
            return COMPOSE_PREFIX;
        } else {
            return COMPOSE_PREFIX + convertToYml(dockerComposeData);
        }
    }

    @Override
    public String buildRancherComposeConfig(List<? extends Service> services, boolean combined) {
        if (combined) {
            return "";
        }
        Map<String, Object> dockerComposeData = createComposeData(services, false, new ArrayList<>(), combined);
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
            List<? extends VolumeTemplate> volumes, boolean combined) {
        Map<String, Object> servicesData = new HashMap<>();
        List<Long> servicesToExportIds = servicesToExport.stream().map(Service::getId).collect(toList());
        Map<String, Object> volumesData = new HashMap<>();
        for (Service service : servicesToExport) {
            List<String> launchConfigNames = ServiceUtil.getLaunchConfigNames(service);
            for (String launchConfigName : launchConfigNames) {
                boolean isPrimaryConfig = launchConfigName
                        .equals(ServiceConstants.PRIMARY_LAUNCH_CONFIG_NAME);
                Map<String, Object> cattleServiceData = ServiceUtil.getLaunchConfigWithServiceDataAsMap(
                        service, launchConfigName);
                Map<String, Object> composeServiceData = new HashMap<>();
                excludeRancherHash(cattleServiceData);
                formatScale(cattleServiceData);
                formatLBConfig(service, cattleServiceData);
                setupServiceType(cattleServiceData);
                for (String cattleService : cattleServiceData.keySet()) {
                    translateRancherToCompose(forDockerCompose, cattleServiceData, composeServiceData, cattleService,
                            service, false, combined);
                }

                if (forDockerCompose || combined) {
                    populateLinksForService(service, servicesToExportIds, composeServiceData);
                    populateNetworkForService(service, launchConfigName, composeServiceData);
                    populateVolumesForService(service, launchConfigName, composeServiceData);
                    addExtraComposeParameters(service, composeServiceData);
                    populateSidekickLabels(service, composeServiceData, isPrimaryConfig);
                    populateSelectorServiceLabels(service, composeServiceData);
                    populateLogConfig(cattleServiceData, composeServiceData);
                    populateTmpfs(cattleServiceData, composeServiceData);
                    populateUlimit(cattleServiceData, composeServiceData);
                    populateBlkioOptions(cattleServiceData, composeServiceData);
                    translateV1VolumesToV2(cattleServiceData, composeServiceData, volumesData, combined);
                }
                if (!composeServiceData.isEmpty()) {
                    servicesData.put(isPrimaryConfig ? service.getName() : launchConfigName, composeServiceData);
                }
            }
        }

        for (VolumeTemplate volume : volumes) {
            Map<String, Object> cattleVolumeData = new HashMap<>();
            cattleVolumeData.putAll(DataAccessor.getFields(volume));
            cattleVolumeData.put(ServiceConstants.FIELD_VOLUME_EXTERNAL, volume.getExternal());
            cattleVolumeData.put(ServiceConstants.FIELD_VOLUME_DRIVER, volume.getDriver());
            cattleVolumeData.put(ServiceConstants.FIELD_VOLUME_PER_CONTAINER, volume.getPerContainer());
            Map<String, Object> composeVolumeData = new HashMap<>();
            for (String cattleVolume : cattleVolumeData.keySet()) {
                translateRancherToCompose(forDockerCompose, cattleVolumeData, composeVolumeData, cattleVolume,
                        null, true, combined);
            }
            if (!composeVolumeData.isEmpty()) {
                volumesData.put(volume.getName(), composeVolumeData);
            }
        }

        Map<String, Object> data = new HashMap<>();
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
    protected void populateSelectorServiceLabels(Service service,
                                                 Map<String, Object> composeServiceData) {
        String selectorContainer = service.getSelector();
        if (selectorContainer == null) {
            return;
        }

        Map<String, String> labels = new HashMap<>();
        if (composeServiceData.get(InstanceConstants.FIELD_LABELS) != null) {
            labels.putAll((HashMap<String, String>) composeServiceData.get(InstanceConstants.FIELD_LABELS));
        }
        labels.put(ServiceConstants.LABEL_SELECTOR_CONTAINER, selectorContainer);

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
        if (service.getKind().equalsIgnoreCase(ServiceConstants.KIND_LOAD_BALANCER_SERVICE)) {
            return;
        }
        List<String> serviceLinksWithNames = new ArrayList<>();
        List<String> externalLinksServices = new ArrayList<>();
        Map<String, ?> links = DataAccessor.fieldMapRO(service, ServiceConstants.FIELD_SERVICE_LINKS);
        for (String link : links.keySet()) {
            Service consumedService = objectManager.findOne(Service.class, SERVICE.ID, links.get(link));
            if (consumedService == null) {
                continue;
            }
            String linkName = consumedService.getName();
            if (!linkName.equals(link)) {
                linkName += ":" + link;
            }
            if (servicesToExportIds.contains(consumedService.getId())) {
                serviceLinksWithNames.add(linkName);
            } else if (!consumedService.getStackId().equals(service.getStackId())) {
                Stack externalStack = objectManager.loadResource(Stack.class,
                        consumedService.getStackId());
                externalLinksServices.add(externalStack.getName() + "/" + linkName);
            }
        }
        if (!serviceLinksWithNames.isEmpty()) {
            composeServiceData.put(ComposeExportConfigItem.LINKS.getDockerName(), serviceLinksWithNames);
        }

        if (!externalLinksServices.isEmpty()) {
            composeServiceData.put(ComposeExportConfigItem.EXTERNALLINKS.getDockerName(), externalLinksServices);
        }
    }

    private void addExtraComposeParameters(Service service,
                                           Map<String, Object> composeServiceData) {
        if (service.getKind().equalsIgnoreCase(ServiceConstants.KIND_DNS_SERVICE)) {
            composeServiceData.put(ComposeExportConfigItem.IMAGE.getDockerName(), ServiceConstants.IMAGE_DNS);
        } else if (service.getKind().equalsIgnoreCase(ServiceConstants.KIND_EXTERNAL_SERVICE)) {
            composeServiceData.put(ComposeExportConfigItem.IMAGE.getDockerName(), "rancher/external-service");
        }
    }

    private void populateNetworkForService(Service service,
            String launchConfigName, Map<String, Object> composeServiceData) {
        Object networkMode = composeServiceData.get(ComposeExportConfigItem.NETWORKMODE.getDockerName());
        if (networkMode != null) {
            if (networkMode.equals(NetworkConstants.NETWORK_MODE_CONTAINER)) {
                Map<String, Object> serviceData = ServiceUtil.getLaunchConfigDataAsMap(service,
                        launchConfigName);
                // network mode can be passed by container, or by service name, so check both
                // networkFromContainerId wins
                Integer targetContainerId = DataAccessor
                        .fieldInteger(service, InstanceConstants.DOCKER_CONTAINER);
                if (targetContainerId != null) {
                    Instance instance = objectManager.loadResource(Instance.class, targetContainerId.longValue());
                    String instanceName = ServiceUtil.getInstanceName(instance);
                    composeServiceData.put(ComposeExportConfigItem.NETWORKMODE.getDockerName(),
                            NetworkConstants.NETWORK_MODE_CONTAINER + ":" + instanceName);
                } else {
                    Object networkLaunchConfig = serviceData
                            .get(InstanceConstants.FIELD_NETWORK_CONTAINER_ID);
                    if (networkLaunchConfig != null) {
                        composeServiceData.put(ComposeExportConfigItem.NETWORKMODE.getDockerName(),
                                NetworkConstants.NETWORK_MODE_CONTAINER + ":" + networkLaunchConfig);
                    }
                }
            } else if (networkMode.equals(NetworkConstants.NETWORK_MODE_MANAGED)) {
                composeServiceData.remove(ComposeExportConfigItem.NETWORKMODE.getDockerName());
            }
        }
    }

    protected void translateRancherToCompose(boolean forDockerCompose, Map<String, Object> rancherServiceData,
            Map<String, Object> composeServiceData, String cattleName, Service service, boolean isVolume, boolean combined) {
        ComposeExportConfigItem item = ComposeExportConfigItem.getServiceConfigItemByCattleName(cattleName,
                service, isVolume);
        if (item != null && (item.isDockerComposeProperty() == forDockerCompose || combined)) {
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
                if ((Boolean) value) {
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
                InstanceConstants.FIELD_VOLUMES_FROM);

        if (dataVolumesLaunchConfigs != null) {
            launchConfigNames.addAll((List<String>) dataVolumesLaunchConfigs);
        }

        // 1. add launch config names
        namesCombined.addAll(launchConfigNames);

        // 2. add instance names if specified
        List<? extends Integer> instanceIds = (List<? extends Integer>) launchConfigData
                .get(InstanceConstants.FIELD_VOLUMES_FROM);

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
            composeServiceData.put(ComposeExportConfigItem.VOLUMESFROM.getDockerName(), namesCombined);
        }
    }

    @SuppressWarnings("unchecked")
    private void translateV1VolumesToV2(Map<String, Object> cattleServiceData,
            Map<String, Object> composeServiceData, Map<String, Object> volumesData, boolean combined) {
        // volume driver presence defines the v1 format for the volumes
        String volumeDriver = String.valueOf(cattleServiceData.get(ComposeExportConfigItem.VOLUME_DRIVER
                .getCattleName()));
        if (StringUtils.isEmpty(volumeDriver)) {
            return;
        }
        composeServiceData.remove(ComposeExportConfigItem.VOLUME_DRIVER
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
                        null, true, combined);
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
                for (String key : map.keySet()) {
                    if (key.equalsIgnoreCase("config") && map.get(key) != null) {
                        if (map.get(key) instanceof Map && !((Map<?, ?>) map.get(key)).isEmpty()) {
                            logConfig.put("options", map.get(key));
                        }
                    } else if (key.equalsIgnoreCase("driver") && map.get(key) != null && StringUtils.isNotBlank(map.get(key).toString())) {
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
            LbConfig lbConfig = DataAccessor.field(service, ServiceConstants.FIELD_LB_CONFIG, LbConfig.class);
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
    protected void formatScale(Map<String, Object> composeServiceData) {
        if (composeServiceData.get(InstanceConstants.FIELD_LABELS) != null) {
            Map<String, String> labels = ((HashMap<String, String>) composeServiceData
                    .get(InstanceConstants.FIELD_LABELS));
            String globalService = labels.get(ServiceConstants.LABEL_SERVICE_GLOBAL);
            if (Boolean.valueOf(globalService)) {
                composeServiceData.remove(ServiceConstants.FIELD_SCALE);
            } else {
                composeServiceData.remove(ServiceConstants.FIELD_SCALE_MAX);
                composeServiceData.remove(ServiceConstants.FIELD_SCALE_MIN);
                composeServiceData.remove(ServiceConstants.FIELD_SCALE_INCREMENT);
            }
        }
    }

    protected void setupServiceType(Map<String, Object> composeServiceData) {
        Object type = composeServiceData.get(ComposeExportConfigItem.SERVICE_TYPE.getCattleName());
        if (type == null) {
            return;
        }
        if (!InstanceConstants.KIND_VIRTUAL_MACHINE.equals(type.toString())) {
            composeServiceData.remove(ComposeExportConfigItem.SERVICE_TYPE.getCattleName());
        }
    }
}
