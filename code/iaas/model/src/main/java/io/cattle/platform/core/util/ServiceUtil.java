package io.cattle.platform.core.util;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.addon.InstanceHealthCheck;
import io.cattle.platform.core.constants.AgentConstants;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.NetworkConstants;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.ServiceRevision;
import io.cattle.platform.core.model.Stack;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.object.util.DataUtils;
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

import com.netflix.config.DynamicStringListProperty;

public class ServiceUtil {

    private static final int LB_HEALTH_CHECK_PORT = 42;
    private static DynamicStringListProperty UPGRADE_TRIGGER_FIELDS = ArchaiusUtil
            .getList("container.upgrade.trigger.fields");
    private static DynamicStringListProperty UPDATE_SKIP_FIELDS = ArchaiusUtil.getList("service.update.skip.fields");

    public static String getInstanceName(Instance instance) {
        if (instance != null && instance.getRemoved() == null) {
            return instance.getUuid();
        } else {
            return null;
        }
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
    public static Map<String, Map<String, Object>> getServiceLaunchConfigsWithNames(Service service) {
        Map<String, Object> originalData = new HashMap<>();
        originalData.putAll(DataUtils.getFields(service));
        Map<String, Map<String, Object>> launchConfigsWithNames = new HashMap<>();

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

    public static String generateServiceInstanceName(Stack env, Service service, String launchConfigName,
            int finalOrder) {
        boolean isPrimary = launchConfigName == null
                || launchConfigName.equals(ServiceConstants.PRIMARY_LAUNCH_CONFIG_NAME);

        if (finalOrder == 0) {
            if (isPrimary) {
                return service.getName();
            }
            return launchConfigName;
        }
        String configName = isPrimary ? "" : launchConfigName + "-";
        return String.format("%s-%s-%s%d", env.getName(), service.getName(), configName, finalOrder);
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

    public static void preserveOldRandomPorts(Service service,
            Map<String, Object> newLaunchConfig) {

        // get from svc
        String lcName = newLaunchConfig.get("name") == null ? ServiceConstants.PRIMARY_LAUNCH_CONFIG_NAME
                : newLaunchConfig.get("name").toString();
        Map<Integer, PortSpec> oldPortMap = getServicePortsMap(ServiceUtil.getLaunchConfigDataAsMap(service, lcName));
        Map<Integer, PortSpec> newPortMap = getServicePortsMap(newLaunchConfig);

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
    protected static Map<Integer, PortSpec> getServicePortsMap(Map<String, Object> launchConfigData) {
        if (launchConfigData.get(InstanceConstants.FIELD_PORTS) == null) {
            return new LinkedHashMap<>();
        }
        List<String> specs = (List<String>) launchConfigData.get(InstanceConstants.FIELD_PORTS);
        Map<Integer, PortSpec> portMap = new LinkedHashMap<>();
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

    public static String getServiceNamespace(Stack stack, Service service) {
        return new StringBuilder().append(service.getName()).append(".").append(getStackNamespace(stack))
                .toString().toLowerCase();
    }

    public static String getStackNamespace(Stack stack) {
        return new StringBuilder().append(stack.getName()).append(".")
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

    public static MergedLaunchConfigs mergeLaunchConfigs(Map<String, Object> currentLaunchConfig,
            List<Map<String, Object>> currentSecondaryLaunchConfigs, Map<String, Object> newPrimaryLaunchConfig,
            List<Map<String, Object>> newSecondaryLaunchConfigs, Service service) {

        if (newPrimaryLaunchConfig == null && newSecondaryLaunchConfigs == null) {
            return null;
        }

        Map<String, Map<String, Object>> currentConfigsMap = new HashMap<>();
        if (currentLaunchConfig != null) {
            currentConfigsMap.put(ServiceConstants.PRIMARY_LAUNCH_CONFIG_NAME, currentLaunchConfig);
        }
        // preserve the initial order of the secondary launch configs
        List<String> secNames = new ArrayList<>();
        if (currentSecondaryLaunchConfigs != null) {
            for (Map<String, Object> lc : currentSecondaryLaunchConfigs) {
                currentConfigsMap.put(lc.get("name").toString(), lc);
            }
            for (Object sec : currentSecondaryLaunchConfigs) {
                secNames.add(CollectionUtils.toMap(sec).get("name").toString());
            }
        }
        boolean isUpgrade = false;
        boolean isUpdate = false;
        // 1. Generate merge configs
        List<Map<String, Object>> mergedConfigs = new ArrayList<>();
        if (newPrimaryLaunchConfig != null) {
            mergedConfigs.add(newPrimaryLaunchConfig);
        }
        if (newSecondaryLaunchConfigs != null && !newSecondaryLaunchConfigs.isEmpty()) {
            for (Map<String, Object> newConfig : newSecondaryLaunchConfigs) {
                Object name = newConfig.get("name");
                Object imageUuid = newConfig.get(InstanceConstants.FIELD_IMAGE_UUID);
                if (service.getSelectorContainer() == null && imageUuid != null
                        && StringUtils.equalsIgnoreCase(ServiceConstants.IMAGE_NONE, imageUuid.toString())) {
                    currentConfigsMap.remove(name);
                    isUpgrade = true;
                    continue;
                }
                mergedConfigs.add(newConfig);
            }
        }

        // 2. Merge configs
        List<String> upgradeTriggerFields = UPGRADE_TRIGGER_FIELDS.get();
        String generatedVersion = io.cattle.platform.util.resource.UUID.randomUUID().toString();
        for (Map<String, Object> mergedConfig : mergedConfigs) {
            boolean resetVersion = false;
            String currentVersion = null;
            String name = mergedConfig.get("name") != null ? mergedConfig.get("name").toString()
                    : ServiceConstants.PRIMARY_LAUNCH_CONFIG_NAME;
            try {

                Map<String, Object> currentConfig = currentConfigsMap.get(name);
                if (currentConfig == null) {
                    resetVersion = true;
                    continue;
                }
                if (mergedConfig.containsKey(ServiceConstants.FIELD_FORCE_UPGRADE)
                        && Boolean.valueOf(mergedConfig.get(ServiceConstants.FIELD_FORCE_UPGRADE).toString())) {
                    resetVersion = true;
                }
                currentVersion = String.valueOf(currentConfig.get(ServiceConstants.FIELD_VERSION));
                for (String key : mergedConfig.keySet()) {
                    if (currentConfig.containsKey(key)) {
                        if (key.equalsIgnoreCase(InstanceConstants.FIELD_PORTS)) {
                            preserveOldRandomPorts(service, mergedConfig);
                        }
                        // if field triggers upgrade + value changed, trigger upgrade
                        if (!areObjectsEqual(currentConfig.get(key), mergedConfig.get(key))) {
                            if (upgradeTriggerFields.contains(key)) {
                                resetVersion = true;
                            } else {
                                isUpdate = true;
                            }
                        }
                        currentConfig.remove(key);
                    } else if (mergedConfig.get(key) != null) {
                        if (upgradeTriggerFields.contains(key)) {
                            resetVersion = true;
                        } else {
                            isUpdate = true;
                        }
                    }
                }
                mergedConfig.putAll(currentConfig);
                currentConfigsMap.remove(name);
            } finally {
                if (resetVersion) {
                    setVersion(mergedConfig, generatedVersion);
                    isUpgrade = true;
                } else {
                    setVersion(mergedConfig, currentVersion);
                }
            }
        }

        mergedConfigs.addAll(currentConfigsMap.values());

        return new MergedLaunchConfigs(secNames, mergedConfigs, isUpgrade, isUpdate);
    }

    protected static void setVersion(Map<String, Object> config, String version) {
        config.put(ServiceConstants.FIELD_VERSION, version);
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Map<String, Object>> getCurrentLaunchConfigs(Service service) {
        Map<String, Map<String, Object>> currentLaunchConfigs = new HashMap<>();
        for (Object secondaryLaunchConfigObject : DataAccessor.fields(service)
                .withKey(ServiceConstants.FIELD_SECONDARY_LAUNCH_CONFIGS)
                .withDefault(Collections.EMPTY_LIST).as(
                        List.class)) {
            Map<String, Object> lc = new HashMap<>();
            lc.putAll(CollectionUtils.toMap(secondaryLaunchConfigObject));
            currentLaunchConfigs.put(lc.get("name").toString(), lc);
        }
        Map<String, Object> lc = new HashMap<>();
        lc.putAll(DataAccessor.fields(service)
                .withKey(ServiceConstants.FIELD_LAUNCH_CONFIG).withDefault(Collections.EMPTY_MAP)
                .as(Map.class));
        currentLaunchConfigs.put(service.getName(), lc);
        return currentLaunchConfigs;
    }

    public static class MergedLaunchConfigs {
        private Map<String, Object> primaryLaunchConfig;
        private List<Map<String, Object>> secondaryLaunchConfigs;
        private boolean isUpgrade;
        private boolean isUpdate;

        public MergedLaunchConfigs(List<String> currentOrderedSecondaryConfigNames,
                List<Map<String, Object>> newLaunchConfigs, boolean isUpgrade, boolean isUpdate) {
            super();

            Map<String, Map<String, Object>> secondaryLCTemp = new HashMap<>();

            for (Map<String, Object> lc : newLaunchConfigs) {
                if (!lc.containsKey("name")) {
                    this.primaryLaunchConfig = lc;
                } else {
                    secondaryLCTemp.put(lc.get("name").toString(), lc);
                }
            }

            secondaryLaunchConfigs = new ArrayList<>();
            for (String secName : currentOrderedSecondaryConfigNames) {
                if (secondaryLCTemp.containsKey(secName)) {
                    secondaryLaunchConfigs.add(secondaryLCTemp.get(secName));
                    secondaryLCTemp.remove(secName);
                }
            }
            // add the rest
            secondaryLaunchConfigs.addAll(secondaryLCTemp.values());
            this.isUpgrade = isUpgrade;
            this.isUpdate = isUpdate;
        }

        public Map<String, Object> getPrimaryLaunchConfig() {
            return primaryLaunchConfig;
        }

        public List<Map<String, Object>> getSecondaryLaunchConfigs() {
            return secondaryLaunchConfigs;
        }

        public boolean isUpgrade() {
            return isUpgrade;
        }

        public boolean isUpdate() {
            return isUpdate;
        }
    }

    public static final List<String> getServiceImagesToPrePull(Service service) {
        List<String> images = new ArrayList<>();
        for (String lcName : getLaunchConfigNames(service)) {
            Object imageUUID = getLaunchConfigObject(service, lcName, InstanceConstants.FIELD_IMAGE_UUID);
            if (imageUUID == null) {
                continue;
            }
            if (isImagePrePullOnLaunchConfig(service, lcName)) {
                images.add(imageUUID.toString());
            }
        }
        return images;
    }

    public static boolean isActiveService(Service service) {
        return getServiceActiveStates().contains(service.getState());
    }

    public static boolean isServiceValidForReconcile(Service service) {
        List<String> activeStates = Arrays.asList(CommonStatesConstants.ACTIVATING, CommonStatesConstants.ACTIVE, CommonStatesConstants.UPDATING_ACTIVE);
        return service != null
                && (activeStates.contains(service.getState()))
                && !service.getIsUpgrade();
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
        Map<String, String> serviceLabels = getMergedServiceLabels(service);
        String globalService = serviceLabels.get(ServiceConstants.LABEL_SERVICE_GLOBAL);
        return Boolean.valueOf(globalService);
    }

    public static boolean isImagePrePull(Service service) {
        for (String lc : ServiceUtil.getLaunchConfigNames(service)) {
            if (isImagePrePullOnLaunchConfig(service, lc)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isImagePrePullOnLaunchConfig(Service service, String lc) {
        Object prePull = ServiceUtil.getLaunchConfigObject(service, lc, InstanceConstants.FIELD_IMAGE_PRE_PULL);
        if (prePull != null && Boolean.valueOf(prePull.toString())) {
            return true;
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

    public static boolean areObjectsEqual(Object a, Object b) {
        // covers both being nulls and primitives
        if (a == b) {
            return true;
        }

        // covers both being not null
        if ((a != null) != (b != null)) {
            if (a instanceof String) {
                if (b == null) {
                    b = "";
                }
            } else if (b instanceof String) {
                if (a == null) {
                    a = "";
                }
            } else {
                return false;
            }
        }

        if (a instanceof String || a instanceof Long || a instanceof Integer || a instanceof Boolean) {
            return StringUtils.equalsIgnoreCase(a.toString(), b.toString());
        } else if (a instanceof List) {
            return compareLists(a, b);
        } else if (a instanceof Map) {
            return compareMaps(CollectionUtils.toMap(a), CollectionUtils.toMap(b));
        } else {
            areObjectsEqual(CollectionUtils.toMap(a), CollectionUtils.toMap(b));
        }

        return false;
    }

    @SuppressWarnings("unchecked")
    public static boolean compareLists(Object a, Object b) {
        for (Object aItem : (List<Object>) a) {
            boolean found = false;
            for (Object bItem : (List<Object>) b) {
                if (areObjectsEqual(aItem, bItem)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }
        }

        for (Object bItem : (List<Object>) b) {
            boolean found = false;
            for (Object aItem : (List<Object>) a) {
                if (areObjectsEqual(aItem, bItem)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }
        }
        return true;
    }

    public static boolean compareMaps(Map<Object, Object> a, Map<Object, Object> b) {
        if (!compareMap(a, b)) {
            return false;
        }
        if (!compareMap(b, a)) {
            return false;
        }

        return true;
    }

    public static boolean compareMap(Map<Object, Object> aMap, Map<Object, Object> bMap) {
        for (Object aKey : aMap.keySet()) {
            if (!bMap.containsKey(aKey)) {
                return false;
            }

            if (!areObjectsEqual(aMap.get(aKey), bMap.get(aKey))) {
                return false;
            }
        }
        return true;
    }

    public static Pair<Long, Long> getBatchSizeAndInterval(Service service) {
        Long batchSize = DataAccessor.fieldLong(service, ServiceConstants.FIELD_BATCHSIZE);
        Long intervalMillis = DataAccessor.fieldLong(service, ServiceConstants.FIELD_INTERVAL_MILLISEC);
        if (batchSize == null) {
            batchSize = 1L;
        }

        if (intervalMillis == null) {
            intervalMillis = 2000L;
        }
        return Pair.of(batchSize, intervalMillis);
    }

    public static class RevisionData {
        Map<String, Object> config;
        boolean isUpdate;
        boolean isUpgrade;
        Long revisionId;

        public RevisionData(Map<String, Object> config, boolean isUpdate, boolean isUpgrade) {
            super();
            this.config = config;
            this.isUpdate = isUpdate;
            this.isUpgrade = isUpgrade;
        }

        public Map<String, Object> getConfig() {
            return config;
        }

        public boolean isUpdate() {
            return isUpdate;
        }

        public boolean isUpgrade() {
            return isUpgrade;
        }

        public Long getRevisionId() {
            return revisionId;
        }

        public void setRevisionId(Long revisionId) {
            this.revisionId = revisionId;
        }
    }

    public static RevisionData generateNewRevisionData(Service service, ServiceRevision currentRevision,
            Map<String, Object> newData) {
        Map<String, Object> finalData = new HashMap<>();
        finalData.putAll(newData);
        if (currentRevision == null) {
            return new RevisionData(finalData, false, false);
        }

        Map<String, Object> currentData = new HashMap<>();
        currentData.putAll(CollectionUtils.toMap(DataAccessor.field(currentRevision,
                InstanceConstants.FIELD_REVISION_CONFIG, Object.class)));
        // 1. process launchConfigs separately
        MergedLaunchConfigs mergedConfig = processLaunchConfigs(service, finalData, currentData);

        // 2. process top level service keys
        List<String> updateSkipFields = UPDATE_SKIP_FIELDS.get();
        boolean isUpdate = mergedConfig.isUpdate();
        for (String key : finalData.keySet()) {
            if (updateSkipFields.contains(key)) {
                continue;
            }
            if (currentData.containsKey(key)) {
                if (!areObjectsEqual(currentData.get(key), finalData.get(key))) {
                    isUpdate = true;
                }
                currentData.remove(key);
            } else if (finalData.get(key) != null) {
                isUpdate = true;
            }
        }

        finalData.putAll(currentData);
        finalData.put(ObjectMetaDataManager.ACCOUNT_FIELD, service.getAccountId());
        finalData.put(InstanceConstants.FIELD_SERVICE_ID, service.getId());
        return new RevisionData(finalData, isUpdate, mergedConfig.isUpgrade());
    }

    @SuppressWarnings("unchecked")
    public static MergedLaunchConfigs processLaunchConfigs(Service service, Map<String, Object> finalData,
            Map<String, Object> currentData) {
        Map<String, Object> newPrimaryLaunchConfig = null;
        if (finalData.get(ServiceConstants.FIELD_LAUNCH_CONFIG) != null) {
            newPrimaryLaunchConfig = (Map<String, Object>) finalData.get(ServiceConstants.FIELD_LAUNCH_CONFIG);
        }
        List<Map<String, Object>> newSecondaryLaunchConfigs = null;
        if (finalData.get(ServiceConstants.FIELD_SECONDARY_LAUNCH_CONFIGS) != null) {
            newSecondaryLaunchConfigs = (List<Map<String, Object>>) finalData
                    .get(ServiceConstants.FIELD_SECONDARY_LAUNCH_CONFIGS);
        }

        Map<String, Object> currentPrimaryLaunchConfig = null;
        if (currentData.get(ServiceConstants.FIELD_LAUNCH_CONFIG) != null) {
            currentPrimaryLaunchConfig = (Map<String, Object>) currentData.get(ServiceConstants.FIELD_LAUNCH_CONFIG);
        }

        List<Map<String, Object>> currentSecondaryLaunchConfigs = null;
        if (currentData.get(ServiceConstants.FIELD_SECONDARY_LAUNCH_CONFIGS) != null) {
            currentSecondaryLaunchConfigs = (List<Map<String, Object>>) currentData
                    .get(ServiceConstants.FIELD_SECONDARY_LAUNCH_CONFIGS);
        }
        MergedLaunchConfigs mergedConfig = ServiceUtil.mergeLaunchConfigs(currentPrimaryLaunchConfig,
                currentSecondaryLaunchConfigs, newPrimaryLaunchConfig,
                newSecondaryLaunchConfigs, service);

        currentData.remove(ServiceConstants.FIELD_LAUNCH_CONFIG);
        currentData.remove(ServiceConstants.FIELD_SECONDARY_LAUNCH_CONFIGS);
        finalData.put(ServiceConstants.FIELD_LAUNCH_CONFIG, mergedConfig.getPrimaryLaunchConfig());
        finalData.put(ServiceConstants.FIELD_SECONDARY_LAUNCH_CONFIGS, mergedConfig.getSecondaryLaunchConfigs());
        return mergedConfig;
    }
}
