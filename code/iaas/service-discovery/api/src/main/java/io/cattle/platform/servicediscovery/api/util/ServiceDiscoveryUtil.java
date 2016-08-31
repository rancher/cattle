package io.cattle.platform.servicediscovery.api.util;

import io.cattle.platform.allocator.service.AllocatorService;
import io.cattle.platform.core.addon.InServiceUpgradeStrategy;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.model.Stack;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.ServiceConsumeMap;
import io.cattle.platform.core.model.ServiceExposeMap;
import io.cattle.platform.core.util.PortSpec;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.object.util.DataUtils;
import io.cattle.platform.servicediscovery.api.constants.ServiceDiscoveryConstants;
import io.cattle.platform.servicediscovery.api.resource.ServiceDiscoveryConfigItem;
import io.cattle.platform.util.type.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

public class ServiceDiscoveryUtil {
    public static String getInstanceName(Instance instance) {
        if (instance != null && instance.getRemoved() == null) {
            return instance.getUuid();
        } else {
            return null;
        }
    }

    public static Map<String, Object> getServiceDataAsMap(Service service, String launchConfigName, AllocatorService allocatorService) {
        Map<String, Object> data = new HashMap<>();
        data.putAll(DataUtils.getFields(service));

        // 1) remove launchConfig/secondaryConfig data
        Object launchConfig = data
                .get(ServiceDiscoveryConstants.FIELD_LAUNCH_CONFIG);
        if (launchConfig != null) {
            data.remove(ServiceDiscoveryConstants.FIELD_LAUNCH_CONFIG);
        }

        Object secondaryLaunchConfigs = data
                .get(ServiceDiscoveryConstants.FIELD_SECONDARY_LAUNCH_CONFIGS);
        if (secondaryLaunchConfigs != null) {
            data.remove(ServiceDiscoveryConstants.FIELD_SECONDARY_LAUNCH_CONFIGS);
        }
        // 2) populate launch config data
        data.putAll(getLaunchConfigDataWLabelsUnion(service, launchConfigName, allocatorService));

        return data;
    }

    @SuppressWarnings("unchecked")
    public static List<String> getServiceLaunchConfigNames(Service service) {
        Map<String, Object> originalData = new HashMap<>();
        originalData.putAll(DataUtils.getFields(service));
        List<String> launchConfigNames = new ArrayList<>();

        // put the primary config in
        launchConfigNames.add(ServiceDiscoveryConstants.PRIMARY_LAUNCH_CONFIG_NAME);

        // put the secondary configs in
        Object secondaryLaunchConfigs = originalData
                .get(ServiceDiscoveryConstants.FIELD_SECONDARY_LAUNCH_CONFIGS);
        if (secondaryLaunchConfigs != null) {
            for (Map<String, Object> secondaryLaunchConfig : (List<Map<String, Object>>) secondaryLaunchConfigs) {
                launchConfigNames.add(String.valueOf(secondaryLaunchConfig.get("name")));
            }
        }

        return launchConfigNames;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Map<Object, Object>> getServiceLaunchConfigsWithNames(Service service) {
        Map<String, Object> originalData = new HashMap<>();
        originalData.putAll(DataUtils.getFields(service));
        Map<String, Map<Object, Object>> launchConfigsWithNames = new HashMap<>();

        // put the primary config in
        launchConfigsWithNames.put(ServiceDiscoveryConstants.PRIMARY_LAUNCH_CONFIG_NAME,
                CollectionUtils.toMap(originalData
                        .get(ServiceDiscoveryConstants.FIELD_LAUNCH_CONFIG)));

        // put the secondary configs in
        Object secondaryLaunchConfigs = originalData
                .get(ServiceDiscoveryConstants.FIELD_SECONDARY_LAUNCH_CONFIGS);
        if (secondaryLaunchConfigs != null) {
            for (Map<String, Object> secondaryLaunchConfig : (List<Map<String, Object>>) secondaryLaunchConfigs) {
                launchConfigsWithNames.put(String.valueOf(secondaryLaunchConfig.get("name")),
                        CollectionUtils.toMap(secondaryLaunchConfig));
            }
        }

        return launchConfigsWithNames;
    }

    public static Map<String, String> getServiceLabels(Service service, AllocatorService allocatorService) {
        return getServiceLabels(null, service, allocatorService);
    }

    @SuppressWarnings("unchecked")
    protected static Map<String, String> getServiceLabels(String lcn, Service service, AllocatorService allocatorService) {
        List<String> launchConfigNames = getServiceLaunchConfigNames(service);
        Map<String, String> labelsStr = new HashMap<>();
        for (String currentLaunchConfigName : launchConfigNames) {
            Map<String, Object> data = getLaunchConfigDataAsMap(service, currentLaunchConfigName);
            Object l = data.get(ServiceDiscoveryConfigItem.LABELS.getCattleName());
            if (l != null) {
                Map<String, String> labels = (HashMap<String, String>) l;
                if (lcn == null || lcn.equals(currentLaunchConfigName)) {
                    allocatorService.mergeLabels(labels, labelsStr);
                } else {
                    // Only merge scheduling labels
                    Map<String, String> schedulingLabels = new HashMap<String, String>();
                    for (Map.Entry<String, String> label : labels.entrySet()) {
                        if (StringUtils.startsWith(label.getKey(), "io.rancher.scheduler")) {
                            schedulingLabels.put(label.getKey(), label.getValue());
                        }
                    }
                    allocatorService.mergeLabels(schedulingLabels, labelsStr);
                }
            }
        }

        return labelsStr;
    }

    protected static Map<String, Object> getLaunchConfigDataWLabelsUnion(Service service, String launchConfigName,
            AllocatorService allocatorService) {
        Map<String, Object> launchConfigData = new HashMap<>();

        // 1) get launch config data from the list of primary/secondary
        launchConfigData.putAll(getLaunchConfigDataAsMap(service, launchConfigName));

        // 2. remove labels, and request the union
        launchConfigData.remove(InstanceConstants.FIELD_LABELS);
        launchConfigData.put(InstanceConstants.FIELD_LABELS,
                getServiceLabels(launchConfigName, service, allocatorService));

        return launchConfigData;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, String> getLaunchConfigLabels(Service service, String launchConfigName) {
        if (launchConfigName == null) {
            launchConfigName = ServiceDiscoveryConstants.PRIMARY_LAUNCH_CONFIG_NAME;
        }
        Map<String, Object> data = getLaunchConfigDataAsMap(service, launchConfigName);
        Object labels = data.get(InstanceConstants.FIELD_LABELS);
        if (labels == null) {
            return new HashMap<String, String>();
        }
        return (Map<String, String>) labels;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> getLaunchConfigDataAsMap(Service service, String launchConfigName) {
        if (launchConfigName == null) {
            launchConfigName = ServiceDiscoveryConstants.PRIMARY_LAUNCH_CONFIG_NAME;
        }
        Map<String, Object> launchConfigData = new HashMap<>();
        if (launchConfigName.equalsIgnoreCase(ServiceDiscoveryConstants.PRIMARY_LAUNCH_CONFIG_NAME)) {
            launchConfigData = DataAccessor.fields(service)
                    .withKey(ServiceDiscoveryConstants.FIELD_LAUNCH_CONFIG).withDefault(Collections.EMPTY_MAP)
                    .as(Map.class);
        } else {
            List<Map<String, Object>> secondaryLaunchConfigs = DataAccessor.fields(service)
                    .withKey(ServiceDiscoveryConstants.FIELD_SECONDARY_LAUNCH_CONFIGS)
                    .withDefault(Collections.EMPTY_LIST).as(
                            List.class);
            for (Map<String, Object> secondaryLaunchConfig : secondaryLaunchConfigs) {
                if (secondaryLaunchConfig.get("name").toString().equalsIgnoreCase(launchConfigName)) {
                    launchConfigData = secondaryLaunchConfig;
                    break;
                }
            }
        }
        Map<String, Object> data = new HashMap<>();
        data.putAll(launchConfigData);

        Object labels = data.get(ServiceDiscoveryConfigItem.LABELS.getCattleName());
        if (labels != null) {
            Map<String, String> labelsMap = new HashMap<String, String>();
            labelsMap.putAll((Map<String, String>) labels);

            // overwrite with a copy of the map
            data.put(ServiceDiscoveryConfigItem.LABELS.getCattleName(), labelsMap);
        }
        return data;
    }

    public static Object getLaunchConfigObject(Service service, String launchConfigName, String objectName) {
        Map<String, Object> serviceData = ServiceDiscoveryUtil.getLaunchConfigDataAsMap(service, launchConfigName);
        return serviceData.get(objectName);
    }

    public static String generateServiceInstanceName(Stack env, Service service, String launchConfigName,
            int finalOrder) {
        String configName = launchConfigName == null
                || launchConfigName.equals(ServiceDiscoveryConstants.PRIMARY_LAUNCH_CONFIG_NAME) ? ""
                : launchConfigName + "_";
        String name = String.format("%s_%s_%s%d", env.getName(), service.getName(), configName, finalOrder);
        return name;
    }

    public static boolean isServiceGeneratedName(Stack env, Service service, String instanceName) {
        return instanceName.startsWith(String.format("%s_%s", env.getName(), service.getName()));
    }

    public static String getGeneratedServiceIndex(Stack env, Service service,
            String launchConfigName,
            String instanceName) {
        if (!isServiceGeneratedName(env, service, instanceName)) {
            return null;
        }
        Integer charAt = instanceName.length()-1;
        for (int i = instanceName.length() - 1; i > 0; i--) {
            if (instanceName.charAt(i) == '_') {
                break;
            }
            charAt = i;
        }
        return instanceName.substring(charAt, instanceName.length());
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> buildServiceInstanceLaunchData(Service service, Map<String, Object> deployParams,
            String launchConfigName, AllocatorService allocatorService) {
        Map<String, Object> serviceData = ServiceDiscoveryUtil.getLaunchConfigDataWLabelsUnion(service,
                launchConfigName, allocatorService);
        Map<String, Object> launchConfigItems = new HashMap<>();

        // 1. put all parameters retrieved through deployParams
        if (deployParams != null) {
            launchConfigItems.putAll(deployParams);
        }

        // 2. Get parameters defined on the service level (merge them with the ones defined in
        for (String key : serviceData.keySet()) {
            Object dataObj = serviceData.get(key);
            if (launchConfigItems.get(key) != null) {
                if (dataObj instanceof Map) {
                    // unfortunately, need to make an except for labels due to the merging aspect of the values
                    if (key.equalsIgnoreCase(InstanceConstants.FIELD_LABELS)) {
                        allocatorService.normalizeLabels(
                                service.getStackId(),
                                (Map<String, String>) launchConfigItems.get(key),
                                (Map<String, String>) dataObj);
                        allocatorService.mergeLabels((Map<String, String>) launchConfigItems.get(key),
                                (Map<String, String>) dataObj);
                    } else {
                        ((Map<Object, Object>) dataObj).putAll((Map<Object, Object>) launchConfigItems.get(key));
                    }
                } else if (dataObj instanceof List) {
                    for (Object existing : (List<Object>) launchConfigItems.get(key)) {
                        if (!((List<Object>) dataObj).contains(existing)) {
                            ((List<Object>) dataObj).add(existing);
                        }
                    }
                }
            }
            if (dataObj != null) {
                launchConfigItems.put(key, dataObj);
            }
        }

        // 3. add extra parameters
        launchConfigItems.put("accountId", service.getAccountId());
        if (!launchConfigItems.containsKey(ObjectMetaDataManager.KIND_FIELD)) {
            launchConfigItems.put(ObjectMetaDataManager.KIND_FIELD, InstanceConstants.KIND_CONTAINER);
        }

        return launchConfigItems;
    }

    public static String getDnsName(Service service, ServiceConsumeMap serviceConsumeMap,
            ServiceExposeMap serviceExposeMap, boolean self) {

        String dnsPrefix = null;
        if (serviceExposeMap != null) {
            dnsPrefix = serviceExposeMap.getDnsPrefix();
        }

        String consumeMapName = null;
        if (serviceConsumeMap != null) {
            consumeMapName = serviceConsumeMap.getName();
        }

        String primaryDnsName = (consumeMapName != null && !consumeMapName.isEmpty()) ? consumeMapName
                : service.getName();
        String dnsName = primaryDnsName;
        if (self) {
            dnsName = dnsPrefix == null ? dnsName : dnsPrefix;
        } else {
            dnsName = dnsPrefix == null ? dnsName : dnsPrefix + "." + dnsName;
        }

        return dnsName;
    }

    public static boolean isNoopService(Service service, AllocatorService allocatorService) {
        Object imageUUID = ServiceDiscoveryUtil.getServiceDataAsMap(service,
                ServiceDiscoveryConstants.PRIMARY_LAUNCH_CONFIG_NAME, allocatorService).get(
                InstanceConstants.FIELD_IMAGE_UUID);
        return service.getSelectorContainer() != null
                && (imageUUID == null || imageUUID.toString().toLowerCase()
                        .contains(ServiceDiscoveryConstants.IMAGE_NONE));
    }

    public static void upgradeServiceConfigs(Service service, InServiceUpgradeStrategy strategy, boolean rollback) {
        updatePrimaryLaunchConfig(strategy, service, rollback);
        updateSecondaryLaunchConfigs(strategy, service, rollback);
    }

    @SuppressWarnings("unchecked")
    protected static void updateSecondaryLaunchConfigs(InServiceUpgradeStrategy strategy, Service service,
            boolean rollback) {
        Object newLaunchConfigs = null;
        if (rollback) {
            newLaunchConfigs = strategy.getPreviousSecondaryLaunchConfigs();
        } else {
            newLaunchConfigs = strategy.getSecondaryLaunchConfigs();
            Map<String, Map<String, Object>> newLaunchConfigNames = new HashMap<>();
            if (newLaunchConfigs != null) {
                for (Map<String, Object> newLaunchConfig : (List<Map<String, Object>>) newLaunchConfigs) {
                    newLaunchConfigNames.put(newLaunchConfig.get("name").toString(),
                            newLaunchConfig);
                }
                Object oldLaunchConfigs = strategy.getPreviousSecondaryLaunchConfigs();
                for (Map<String, Object> oldLaunchConfig : (List<Map<String, Object>>)oldLaunchConfigs) {
                    Map<String, Object> newLaunchConfig = newLaunchConfigNames
                            .get(oldLaunchConfig.get("name"));
                    if (newLaunchConfig != null) {
                        preserveOldRandomPorts(service, newLaunchConfig, oldLaunchConfig);
                    }
                }
            }
        }

        DataAccessor.fields(service).withKey(ServiceDiscoveryConstants.FIELD_SECONDARY_LAUNCH_CONFIGS)
                .set(newLaunchConfigs);
    }

    @SuppressWarnings("unchecked")
    protected static void updatePrimaryLaunchConfig(InServiceUpgradeStrategy strategy, Service service, boolean rollback) {
        Map<String, Object> newLaunchConfig = null;
        if (rollback) {
            newLaunchConfig = (Map<String, Object>) strategy.getPreviousLaunchConfig();
        } else {
            newLaunchConfig = (Map<String, Object>) strategy.getLaunchConfig();
            Map<String, Object> oldLaunchConfig = (Map<String, Object>) strategy.getPreviousLaunchConfig();
            preserveOldRandomPorts(service, newLaunchConfig, oldLaunchConfig);
        }
        DataAccessor.fields(service).withKey(ServiceDiscoveryConstants.FIELD_LAUNCH_CONFIG)
                .set(newLaunchConfig);
    }

    protected static void preserveOldRandomPorts(Service service, Map<String, Object> newLaunchConfig, Map<String, Object> oldLaunchConfig) {
        Map<Integer, PortSpec> oldPortMap = getServicePortsMap(service, oldLaunchConfig);
        Map<Integer, PortSpec> newPortMap = getServicePortsMap(service, newLaunchConfig);

        boolean changedNewPorts = false;

        for(Integer privatePort : newPortMap.keySet()) {
            if(newPortMap.get(privatePort).getPublicPort() == null) {
                if (oldPortMap.containsKey(privatePort)) {
                    newPortMap.get(privatePort).setPublicPort(oldPortMap.get(privatePort).getPublicPort());
                    changedNewPorts = true;
                }
            }
        }

        if(changedNewPorts) {
            List<String> newPorts = new ArrayList<>();
            for (Map.Entry<Integer, PortSpec> entry : newPortMap.entrySet()) {
                 newPorts.add(entry.getValue().toSpec());
            }
            if (!newPorts.isEmpty()) {
                newLaunchConfig.put(InstanceConstants.FIELD_PORTS, newPorts);
            }
        }
    }

    @SuppressWarnings("unchecked")
    protected static Map<Integer, PortSpec> getServicePortsMap(Service service, Map<String, Object> launchConfigData) {
        if (launchConfigData.get(InstanceConstants.FIELD_PORTS) == null) {
            return new LinkedHashMap<Integer, PortSpec>();
        }
        List<String> specs = (List<String>) launchConfigData.get(InstanceConstants.FIELD_PORTS);
        Map<Integer, PortSpec> portMap = new LinkedHashMap<Integer, PortSpec>();
        for (String spec : specs) {
            boolean defaultProtocol = true;
            if (service.getKind().equalsIgnoreCase(ServiceDiscoveryConstants.KIND_LOAD_BALANCER_SERVICE)) {
                defaultProtocol = false;
            }
            PortSpec portSpec = new PortSpec(spec, defaultProtocol);
            portMap.put(new Integer(portSpec.getPrivatePort()), portSpec);

        }
        return portMap;
    }
}
