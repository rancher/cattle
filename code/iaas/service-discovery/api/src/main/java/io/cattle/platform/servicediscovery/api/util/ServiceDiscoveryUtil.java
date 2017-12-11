package io.cattle.platform.servicediscovery.api.util;

import io.cattle.platform.allocator.service.AllocationHelper;
import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.addon.InServiceUpgradeStrategy;
import io.cattle.platform.core.addon.InstanceHealthCheck;
import io.cattle.platform.core.constants.AgentConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.Stack;
import io.cattle.platform.core.util.PortSpec;
import io.cattle.platform.core.util.SystemLabels;
import io.cattle.platform.docker.client.DockerImage;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.object.util.DataUtils;
import io.cattle.platform.servicediscovery.api.resource.ServiceDiscoveryConfigItem;
import io.cattle.platform.util.type.CollectionUtils;
import io.github.ibuildthecloud.gdapi.validation.ValidationErrorCodes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import com.netflix.config.DynamicStringProperty;

public class ServiceDiscoveryUtil {

    public static final List<String> SERVICE_INSTANCE_NAME_DIVIDORS = Arrays.asList("-", "_");
    private static final int LB_HEALTH_CHECK_PORT = 42;
    private static final DynamicStringProperty LB_DRAIN_IMAGE_VERSION = ArchaiusUtil.getString("loadbalancher.drain.image.version");
    private static final DynamicStringProperty LB_IMAGE_UUID = ArchaiusUtil.getString("lb.instance.image.uuid");

    public static String getInstanceName(Instance instance) {
        if (instance != null && instance.getRemoved() == null) {
            return instance.getUuid();
        } else {
            return null;
        }
    }

    public static String getServiceSuffixFromInstanceName(String instanceName) {
        for (String divider : SERVICE_INSTANCE_NAME_DIVIDORS) {
            if (!instanceName.contains(divider)) {
                continue;
            }
            String serviceSuffix = instanceName.substring(instanceName.lastIndexOf(divider) + 1);
            if (!StringUtils.isEmpty(serviceSuffix) && serviceSuffix.matches("\\d+")) {
                return serviceSuffix;
            }
        }
        return "";
    }

    @SuppressWarnings("unchecked")
    public static List<String> getServiceLaunchConfigNames(Service service) {
        Map<String, Object> originalData = new HashMap<>();
        originalData.putAll(DataUtils.getFields(service));
        List<String> launchConfigNames = new ArrayList<>();

        // put the primary config in
        launchConfigNames.add(ServiceConstants.PRIMARY_LAUNCH_CONFIG_NAME);

        // put the secondary configs in
        Object secondaryLaunchConfigs = originalData
                .get(ServiceConstants.FIELD_SECONDARY_LAUNCH_CONFIGS);
        if (secondaryLaunchConfigs != null) {
            for (Map<String, Object> secondaryLaunchConfig : (List<Map<String, Object>>) secondaryLaunchConfigs) {
                launchConfigNames.add(String.valueOf(secondaryLaunchConfig.get("name")));
            }
        }

        return launchConfigNames;
    }

    public static Map<String, Object> getLaunchConfigWithServiceDataAsMap(Service service, String launchConfigName) {
        Map<String, Object> data = new HashMap<>();
        // 1) get service data
        data.putAll(DataUtils.getFields(service));

        // 2) remove launchConfig/secondaryConfig data
        Object launchConfig = data
                .get(ServiceConstants.FIELD_LAUNCH_CONFIG);
        if (launchConfig != null) {
            data.remove(ServiceConstants.FIELD_LAUNCH_CONFIG);
        }

        Object secondaryLaunchConfigs = data
                .get(ServiceConstants.FIELD_SECONDARY_LAUNCH_CONFIGS);
        if (secondaryLaunchConfigs != null) {
            data.remove(ServiceConstants.FIELD_SECONDARY_LAUNCH_CONFIGS);
        }
        // 3) populate launch config data
        data.putAll(getLaunchConfigDataAsMap(service, launchConfigName));

        return data;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Map<Object, Object>> getServiceLaunchConfigsWithNames(Service service) {
        Map<String, Object> originalData = new HashMap<>();
        originalData.putAll(DataUtils.getFields(service));
        Map<String, Map<Object, Object>> launchConfigsWithNames = new HashMap<>();

        // put the primary config in
        launchConfigsWithNames.put(ServiceConstants.PRIMARY_LAUNCH_CONFIG_NAME,
                CollectionUtils.toMap(originalData
                        .get(ServiceConstants.FIELD_LAUNCH_CONFIG)));

        // put the secondary configs in
        Object secondaryLaunchConfigs = originalData
                .get(ServiceConstants.FIELD_SECONDARY_LAUNCH_CONFIGS);
        if (secondaryLaunchConfigs != null) {
            for (Map<String, Object> secondaryLaunchConfig : (List<Map<String, Object>>) secondaryLaunchConfigs) {
                launchConfigsWithNames.put(String.valueOf(secondaryLaunchConfig.get("name")),
                        CollectionUtils.toMap(secondaryLaunchConfig));
            }
        }

        return launchConfigsWithNames;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, String> getMergedServiceLabels(Service service, AllocationHelper allocationHelper) {
        List<String> launchConfigNames = getServiceLaunchConfigNames(service);
        Map<String, String> labelsStr = new HashMap<>();
        for (String currentLaunchConfigName : launchConfigNames) {
            Map<String, Object> data = getLaunchConfigDataAsMap(service, currentLaunchConfigName);
            Object l = data.get(ServiceDiscoveryConfigItem.LABELS.getCattleName());
            if (l != null) {
                Map<String, String> labels = (HashMap<String, String>) l;
                    allocationHelper.mergeLabels(labels, labelsStr);
            }
        }
        return labelsStr;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, String> getLaunchConfigLabels(Service service, String launchConfigName) {
        if (launchConfigName == null) {
            launchConfigName = ServiceConstants.PRIMARY_LAUNCH_CONFIG_NAME;
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
            launchConfigName = ServiceConstants.PRIMARY_LAUNCH_CONFIG_NAME;
        }
        Map<String, Object> launchConfigData = new HashMap<>();
        if (launchConfigName.equalsIgnoreCase(ServiceConstants.PRIMARY_LAUNCH_CONFIG_NAME)) {
            launchConfigData = DataAccessor.fields(service)
                    .withKey(ServiceConstants.FIELD_LAUNCH_CONFIG).withDefault(Collections.EMPTY_MAP)
                    .as(Map.class);
            // if the value is empty, do not export
            ArrayList<String> deletedKeys = new ArrayList<>();
            for (String key: launchConfigData.keySet()) {
                if (launchConfigData.get(key) == null) {
                    deletedKeys.add(key);
                }
            }
            for (String key: deletedKeys) {
                launchConfigData.remove(key);
            }
        } else {
            List<Map<String, Object>> secondaryLaunchConfigs = DataAccessor.fields(service)
                    .withKey(ServiceConstants.FIELD_SECONDARY_LAUNCH_CONFIGS)
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
                || launchConfigName.equals(ServiceConstants.PRIMARY_LAUNCH_CONFIG_NAME) ? ""
                : launchConfigName + "-";
        String name = String.format("%s-%s-%s%d", env.getName(), service.getName(), configName, finalOrder);
        return name;
    }

    public static boolean isServiceGeneratedName(Stack env, Service service, String instanceName) {
        for (String divider : SERVICE_INSTANCE_NAME_DIVIDORS) {
            if (instanceName.startsWith(String.format("%s%s%s", env.getName(), divider, service.getName()))) {
                return true;
            }
        }
        return false;
    }

    public static String getGeneratedServiceIndex(Stack env, Service service,
            String launchConfigName,
            String instanceName) {
        if (!isServiceGeneratedName(env, service, instanceName)) {
            return null;
        }
        Integer charAt = instanceName.length()-1;
        for (int i = instanceName.length() - 1; i > 0; i--) {
            if (instanceName.charAt(i) == '-' || instanceName.charAt(i) == '_') {
                break;
            }
            charAt = i;
        }
        return instanceName.substring(charAt, instanceName.length());
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> buildServiceInstanceLaunchData(Service service, Map<String, Object> deployParams,
            String launchConfigName, AllocationHelper allocationHelper) {
        Map<String, Object> serviceData = getLaunchConfigDataAsMap(service, launchConfigName);
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
                        allocationHelper.normalizeLabels(
                                service.getStackId(),
                                (Map<String, String>) launchConfigItems.get(key),
                                (Map<String, String>) dataObj);
                        allocationHelper.mergeLabels((Map<String, String>) launchConfigItems.get(key),
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

    public static boolean isNoopService(Service service) {
        Object imageUUID = ServiceDiscoveryUtil.getLaunchConfigDataAsMap(service,
                ServiceConstants.PRIMARY_LAUNCH_CONFIG_NAME).get(
                InstanceConstants.FIELD_IMAGE_UUID);
        return (service.getSelectorContainer() != null
                && (imageUUID == null || imageUUID.toString().toLowerCase()
                .contains(ServiceConstants.IMAGE_NONE))) || isNoopLBService(service);
    }

    public static boolean isNoopLBService(Service service) {
        Object imageUUID = ServiceDiscoveryUtil.getLaunchConfigDataAsMap(service,
                ServiceConstants.PRIMARY_LAUNCH_CONFIG_NAME).get(
                InstanceConstants.FIELD_IMAGE_UUID);
        return service.getKind().equalsIgnoreCase(ServiceConstants.KIND_LOAD_BALANCER_SERVICE)
                && imageUUID != null && imageUUID.toString().toLowerCase()
                        .contains(ServiceConstants.IMAGE_NONE);
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

        DataAccessor.fields(service).withKey(ServiceConstants.FIELD_SECONDARY_LAUNCH_CONFIGS)
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
        DataAccessor.fields(service).withKey(ServiceConstants.FIELD_LAUNCH_CONFIG)
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
            PortSpec portSpec = new PortSpec(spec);
            portMap.put(new Integer(portSpec.getPrivatePort()), portSpec);

        }
        return portMap;
    }

    @SuppressWarnings("unchecked")
    public static void injectBalancerLabelsAndHealthcheck(Map<Object, Object> launchConfig) {
        Map<String, String> labels = new HashMap<>();
        // set labels
        Object labelsObj = launchConfig.get(InstanceConstants.FIELD_LABELS);
        if (labelsObj != null) {
            labels = (Map<String, String>) labelsObj;
        }
        if (!labels.containsKey(SystemLabels.LABEL_AGENT_ROLE)) {
            labels.put(SystemLabels.LABEL_AGENT_ROLE, AgentConstants.ENVIRONMENT_ADMIN_ROLE);
            labels.put(SystemLabels.LABEL_AGENT_CREATE, "true");
        }

        //check if the LB service is a drainProvider from lb image
        if(doesLBHaveDrainSupport(launchConfig)){
            labels.put(SystemLabels.LABEL_AGENT_SERVICE_DRAIN_PROVIDER, "true");
            labels.put(SystemLabels.LABEL_AGENT_ROLE, AgentConstants.ENVIRONMENT_ADMIN_ROLE + ",agent");
            labels.put(SystemLabels.LABEL_AGENT_CREATE, "true");
        }

        launchConfig.put(InstanceConstants.FIELD_LABELS, labels);

        // set health check
        if (launchConfig.get(InstanceConstants.FIELD_HEALTH_CHECK) == null) {
            Integer healthCheckPort = LB_HEALTH_CHECK_PORT;
            InstanceHealthCheck healthCheck = new InstanceHealthCheck();
            healthCheck.setPort(healthCheckPort);
            healthCheck.setInterval(2000);
            healthCheck.setHealthyThreshold(2);
            healthCheck.setUnhealthyThreshold(3);
            healthCheck.setResponseTimeout(2000);
            healthCheck.setInitializingTimeout(60000);
            healthCheck.setReinitializingTimeout(60000);
            launchConfig.put(InstanceConstants.FIELD_HEALTH_CHECK, healthCheck);
        }
    }

    private static boolean doesLBHaveDrainSupport(Map<Object, Object> launchConfig) {
        if(launchConfig.get(InstanceConstants.FIELD_IMAGE_UUID) == null) {
            return false;
        }
        String imageUuid = (String)launchConfig.get(InstanceConstants.FIELD_IMAGE_UUID);
        Pair<String, String> instanceImage = getImageAndVersion(imageUuid.toLowerCase());
        if (instanceImage.getLeft().isEmpty() || instanceImage.getRight().isEmpty()) {
            return false;
        }
        Pair<String, String> defaultImage = getImageAndVersion(LB_IMAGE_UUID.get().toLowerCase());
        if (!defaultImage.getLeft().equals(instanceImage.getLeft())) {
            return false;
        }
        return isDrainProvider(instanceImage.getRight());
    }

    private static Pair<String, String> getImageAndVersion(String imageUUID) {
        DockerImage dockerImage = DockerImage.parse(imageUUID);
        String[] splitted = dockerImage.getFullName().split(":");
        if (splitted.length <= 1) {
            return Pair.of("", "");
        }
        String repoAndImage = splitted[0];
        String imageVersion = splitted[1];
        return Pair.of(repoAndImage, imageVersion);
    }

    private static boolean isDrainProvider(String actualVersion) {
        String requiredVersion = LB_DRAIN_IMAGE_VERSION.get();
        if (StringUtils.isEmpty(requiredVersion)) {
            return false;
        }
        String[] requiredParts = requiredVersion.split("\\.");
        if (requiredParts.length < 3) {
            // Required image is not following semantic versioning.
            return false;
        }
        int requiredMajor, requiredMinor, requiredPatch = 0;
        try {
            String majorTemp = requiredParts[0].startsWith("v") ? requiredParts[0].substring(1, requiredParts[0].length()) : requiredParts[0];
            requiredMajor = Integer.valueOf(majorTemp);
            requiredMinor = Integer.valueOf(requiredParts[1]);
            requiredPatch = Integer.valueOf(requiredParts[2]);
        } catch (NumberFormatException e) {
            // Require image is not following semantic versioning.
            return false;
        }

        String[] actualParts = actualVersion.split("\\.");
        if (actualParts.length < 3) {
            // Image is not following semantic versioning.
            return false;
        }

        int actualMajor, actualMinor, actualPatch = 0;
        try {
            String majorTemp = actualParts[0].startsWith("v") ? actualParts[0].substring(1, actualParts[0].length()) : actualParts[0];
            actualMajor = Integer.valueOf(majorTemp).intValue();
            actualMinor = Integer.valueOf(actualParts[1]).intValue();
            String[] patchParts = actualParts[2].split("\\-");
            actualPatch = Integer.valueOf(patchParts[0]);
        } catch (NumberFormatException e) {
            // Image is not following semantic versioning.
            return false;
        }

        if (actualMajor > requiredMajor) {
            return true;
        } else if (actualMajor == requiredMajor && actualMinor > requiredMinor) {
            return true;
        } else if (actualMinor == requiredMinor && actualPatch >= requiredPatch) {
            return true;
        }
        return false;
    }


    @SuppressWarnings("unchecked")
    public static void validateScaleSwitch(Object newLaunchConfig, Object currentLaunchConfig) {
        if (isGlobalService((Map<Object, Object>) currentLaunchConfig) != isGlobalService((Map<Object, Object>) newLaunchConfig)) {
            ValidationErrorCodes.throwValidationError(ValidationErrorCodes.INVALID_OPTION,
                    "Switching from global scale to fixed (and vice versa)");
        }
    }

    @SuppressWarnings("unchecked")
    protected static boolean isGlobalService(Map<Object, Object> launchConfig) {
        // set labels
        Object labelsObj = launchConfig.get(InstanceConstants.FIELD_LABELS);
        if (labelsObj == null) {
            return false;

        }
        Map<String, String> labels = (Map<String, String>) labelsObj;
        String globalService = labels.get(ServiceConstants.LABEL_SERVICE_GLOBAL);
        return Boolean.valueOf(globalService);
    }
}
