package io.cattle.platform.core.util;

import io.cattle.platform.core.addon.InstanceHealthCheck;
import io.cattle.platform.core.constants.AgentConstants;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.NetworkConstants;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.object.util.DataUtils;
import io.cattle.platform.object.util.ObjectUtils;
import io.cattle.platform.util.type.CollectionUtils;
import io.github.ibuildthecloud.gdapi.validation.ValidationErrorCodes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

public class ServiceUtil {

    private static final int LB_HEALTH_CHECK_PORT = 42;

    public static String getInstanceName(Instance instance) {
        if (instance != null && instance.getRemoved() == null) {
            return instance.getUuid();
        } else {
            return null;
        }
    }

    public static Set<String> getLaunchConfigExternalNames(Map<String, Object> data) {
        Set<String> names = new HashSet<>();
        String name = ObjectUtils.toString(data.get(ObjectMetaDataManager.NAME_FIELD));
        names.add(name);
        for (Object o : CollectionUtils.toList(data.get(ServiceConstants.FIELD_SECONDARY_LAUNCH_CONFIGS))) {
            names.add(ObjectUtils.toString(CollectionUtils.toMap(o)));
        }
        return names;
    }

    @SuppressWarnings("unchecked")
    public static List<String> getLaunchConfigNames(Service service) {
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
    public static Map<String, String> getLaunchConfigLabels(Service service, String launchConfigName) {
        if (launchConfigName == null) {
            launchConfigName = ServiceConstants.PRIMARY_LAUNCH_CONFIG_NAME;
        }
        Map<String, Object> data = getLaunchConfigDataAsMap(service, launchConfigName);
        Object labels = data.get(InstanceConstants.FIELD_LABELS);
        if (labels == null) {
            return new HashMap<>();
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

        Object labels = data.get(InstanceConstants.FIELD_LABELS);
        if (labels != null) {
            Map<String, String> labelsMap = new HashMap<>();
            labelsMap.putAll((Map<String, String>) labels);

            // overwrite with a copy of the map
            data.put(InstanceConstants.FIELD_LABELS, labelsMap);
        }
        return data;
    }

    public static Object getLaunchConfigObject(Service service, String launchConfigName, String objectName) {
        Map<String, Object> serviceData = ServiceUtil.getLaunchConfigDataAsMap(service, launchConfigName);
        return serviceData.get(objectName);
    }

    public static String generateServiceInstanceName(String stackName, String serviceName, String launchConfigName,
            String finalOrder) {
        boolean isPrimary = launchConfigName == null
                || launchConfigName.equals(ServiceConstants.PRIMARY_LAUNCH_CONFIG_NAME);

        if ("0".equals(finalOrder)) {
            if (isPrimary) {
                return serviceName;
            }
            return launchConfigName;
        }
        String configName = isPrimary ? "" : launchConfigName + "-";
        return String.format("%s-%s-%s%s", stackName, serviceName, configName, finalOrder);
    }

    public static boolean isNoopService(Service service) {
        Object imageUUID = ServiceUtil.getLaunchConfigDataAsMap(service,
                ServiceConstants.PRIMARY_LAUNCH_CONFIG_NAME).get(
                InstanceConstants.FIELD_IMAGE_UUID);

        return (service.getSelectorContainer() != null
                && (imageUUID == null || imageUUID.toString().toLowerCase()
                .contains(ServiceConstants.IMAGE_NONE))) || isNoopLBService(service);
    }

    public static boolean isNoopLBService(Service service) {
        Object imageUUID = ServiceUtil.getLaunchConfigDataAsMap(service,
                ServiceConstants.PRIMARY_LAUNCH_CONFIG_NAME).get(
                InstanceConstants.FIELD_IMAGE_UUID);
        return service.getKind().equalsIgnoreCase(ServiceConstants.KIND_LOAD_BALANCER_SERVICE)
                && imageUUID != null && imageUUID.toString().toLowerCase()
                        .contains(ServiceConstants.IMAGE_NONE);
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

    protected static String getGlobalNamespace() {
        return NetworkConstants.INTERNAL_DNS_SEARCH_DOMAIN;
    }

    public static String getServiceNamespace(String stackName, String serviceName) {
        return new StringBuilder().append(serviceName).append(".").append(getStackNamespace(stackName))
                .toString().toLowerCase();
    }

    public static String getStackNamespace(String stackName) {
        return new StringBuilder().append(stackName).append(".")
                .append(getGlobalNamespace()).toString().toLowerCase();
    }

    public static List<String> getServiceActiveStates() {
        return Arrays.asList(CommonStatesConstants.ACTIVATING,
                CommonStatesConstants.ACTIVE, CommonStatesConstants.UPDATING_ACTIVE,
                ServiceConstants.STATE_UPGRADING, ServiceConstants.STATE_ROLLINGBACK,
                ServiceConstants.STATE_PAUSING,
                ServiceConstants.STATE_PAUSED,
                ServiceConstants.STATE_FINISHING_UPGRADE,
                ServiceConstants.STATE_UPGRADED,
                ServiceConstants.STATE_RESTARTING);
    }

    public static boolean isActiveService(Service service) {
        return getServiceActiveStates().contains(service.getState());
    }

    /**
     * Add labels from 'srcMap' to 'destMap'. If key already exists in destMap, either
     * overwrite or merge depending on whether the key is an affinity rule or not
     */
    public static void mergeLabels(Map<String, String> srcMap, Map<String, String> destMap) {
        if (srcMap == null || destMap == null) {
            return;
        }
        for (Map.Entry<String, String> entry : srcMap.entrySet()) {
            String key = entry.getKey();
            if (key.toLowerCase().startsWith("io.rancher")) {
                key = key.toLowerCase();
            }
            String value = entry.getValue();
            if (key.startsWith("io.rancher.scheduler.affinity")) {
                // merge labels
                String destValue = destMap.get(key);
                if (StringUtils.isEmpty(destValue)) {
                    destMap.put(key, value);
                } else if (StringUtils.isEmpty(value)) {
                    continue;
                } else if (!destValue.toLowerCase().contains(value.toLowerCase())) {
                    destMap.put(key, destValue + "," + value);
                }
            } else {
                // overwrite label value
                destMap.put(key, value);
            }
        }
    }

    @SuppressWarnings("unchecked")
    public static Map<String, String> getMergedServiceLabels(Service service) {
        List<String> launchConfigNames = ServiceUtil.getLaunchConfigNames(service);
        Map<String, String> labelsStr = new HashMap<>();
        for (String currentLaunchConfigName : launchConfigNames) {
            Map<String, Object> data = getLaunchConfigDataAsMap(service, currentLaunchConfigName);
            Object l = data.get(InstanceConstants.FIELD_LABELS);
            if (l != null) {
                Map<String, String> labels = (HashMap<String, String>) l;
                mergeLabels(labels, labelsStr);
            }
        }
        return labelsStr;
    }

    public static boolean isGlobalService(Service service) {
        String val = ObjectUtils.toString(CollectionUtils.getNestedValue(service.getData(),
                DataUtils.FIELDS,
                ServiceConstants.FIELD_LAUNCH_CONFIG,
                InstanceConstants.FIELD_LABELS,
                ServiceConstants.LABEL_SERVICE_GLOBAL));
        if (Boolean.valueOf(val)) {
            return true;
        }

        List<?> secondaries = CollectionUtils.toList(CollectionUtils.getNestedValue(service.getData(),
                DataUtils.FIELDS,
                ServiceConstants.FIELD_SECONDARY_LAUNCH_CONFIGS));
        for (Object map : secondaries) {
            val = ObjectUtils.toString(CollectionUtils.getNestedValue(map,
                InstanceConstants.FIELD_LABELS,
                ServiceConstants.LABEL_SERVICE_GLOBAL));
            if (Boolean.valueOf(val)) {
                return true;
            }
        }

        return false;
    }

    public static boolean isV1LB(Service service) {
        if (!service.getKind().equalsIgnoreCase(ServiceConstants.KIND_LOAD_BALANCER_SERVICE)) {
            return false;
        }
        Object lbConfig = DataAccessor.field(service, ServiceConstants.FIELD_LB_CONFIG, Object.class);
        return lbConfig == null;
    }

}
