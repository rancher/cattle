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
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public class ServiceUtil {

    private static final Pattern DNS_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9](?!.*--)[a-zA-Z0-9-]*$");

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
        names.addAll(getLaunchConfigNames(data));
        String name = ObjectUtils.toString(data.get(ObjectMetaDataManager.NAME_FIELD));
        names.add(name);
        names.remove(ServiceConstants.PRIMARY_LAUNCH_CONFIG_NAME);
        return names;
    }

    public static List<String> getLaunchConfigNames(Service service) {
        return getLaunchConfigNames(DataUtils.getFields(service));
    }

    public static List<String> getSidekickNames(Service service) {
        List<String> list = getLaunchConfigNames(DataUtils.getFields(service));
        return list.subList(1, list.size());
    }

    @SuppressWarnings("unchecked")
    private static List<String> getLaunchConfigNames(Map<String, Object> originalData) {
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
    public static Map<String, Object> getLaunchConfigDataAsMap(Service service, String launchConfigName) {
        if (launchConfigName == null) {
            launchConfigName = ServiceConstants.PRIMARY_LAUNCH_CONFIG_NAME;
        }
        Map<String, Object> launchConfigData = new HashMap<>();
        if (launchConfigName.equalsIgnoreCase(ServiceConstants.PRIMARY_LAUNCH_CONFIG_NAME)) {
            for (Map.Entry<String, Object> entry : DataAccessor.fieldMapRO(service, ServiceConstants.FIELD_LAUNCH_CONFIG).entrySet()) {
                if (entry.getValue() != null) {
                    launchConfigData.put(entry.getKey(), entry.getValue());
                }
            }
        } else {
            List<Map<String, Object>> secondaryLaunchConfigs = DataAccessor.fields(service)
                    .withKey(ServiceConstants.FIELD_SECONDARY_LAUNCH_CONFIGS)
                    .withDefault(Collections.EMPTY_LIST)
                    .as(List.class);
            for (Map<String, Object> secondaryLaunchConfig : secondaryLaunchConfigs) {
                if (secondaryLaunchConfig.get("name").toString().equalsIgnoreCase(launchConfigName)) {
                    for (Map.Entry<String, Object> entry : secondaryLaunchConfig.entrySet()) {
                        if (entry.getValue() != null) {
                            launchConfigData.put(entry.getKey(), entry.getValue());
                        }
                    }
                    break;
                }
            }
        }

        Object labels = launchConfigData.get(InstanceConstants.FIELD_LABELS);
        if (labels != null) {
            Map<String, String> labelsMap = new HashMap<>();
            labelsMap.putAll((Map<String, String>) labels);

            // overwrite with a copy of the map
            launchConfigData.put(InstanceConstants.FIELD_LABELS, labelsMap);
        }
        return launchConfigData;
    }

    public static String generateServiceInstanceName(String stackName, String serviceName, String launchConfigName,
            int finalOrder) {
        boolean isPrimary = launchConfigName == null
                || launchConfigName.equals(ServiceConstants.PRIMARY_LAUNCH_CONFIG_NAME);

        if (finalOrder == 0) {
            if (isPrimary) {
                return serviceName;
            }
            return launchConfigName;
        }
        String configName = isPrimary ? "" : launchConfigName + "-";
        return String.format("%s-%s-%s%s", stackName, serviceName, configName, finalOrder);
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
            launchConfig.put(InstanceConstants.FIELD_HEALTH_CHECK, healthCheck);
        }
    }

    private static String getGlobalNamespace() {
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

    public static void validateDNSPatternForName(String name) {
        if (name != null)  {
            if(!DNS_NAME_PATTERN.matcher(name).matches()) {
                ValidationErrorCodes.throwValidationError(ValidationErrorCodes.INVALID_CHARACTERS,
                        "name");
            } else if (name.endsWith("-")){
                ValidationErrorCodes.throwValidationError(ValidationErrorCodes.INVALID_CHARACTERS,
                        "name");
            }
        }
    }

}
