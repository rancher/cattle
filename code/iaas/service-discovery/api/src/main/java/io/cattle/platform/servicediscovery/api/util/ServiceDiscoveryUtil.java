package io.cattle.platform.servicediscovery.api.util;

import io.cattle.platform.allocator.service.AllocationHelper;
import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.addon.InServiceUpgradeStrategy;
import io.cattle.platform.core.addon.InstanceHealthCheck;
import io.cattle.platform.core.constants.AgentConstants;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.NetworkConstants;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.InstanceRevision;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.Stack;
import io.cattle.platform.core.util.PortSpec;
import io.cattle.platform.core.util.SystemLabels;
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

import com.netflix.config.DynamicStringListProperty;

public class ServiceDiscoveryUtil {

    private static final int LB_HEALTH_CHECK_PORT = 42;
    private static DynamicStringListProperty UPGRADE_TRIGGER_FIELDS = ArchaiusUtil.getList("upgrade.trigger.fields");

    public static String getInstanceName(Instance instance) {
        if (instance != null && instance.getRemoved() == null) {
            return instance.getUuid();
        } else {
            return null;
        }
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

    public static String getGeneratedServiceIndex(Stack env, Service service,
            String instanceName) {
        if (!ServiceConstants.isServiceGeneratedName(env, service, instanceName)) {
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

    protected static String getGlobalNamespace(Service service) {
        return NetworkConstants.INTERNAL_DNS_SEARCH_DOMAIN;
    }

    public static String getServiceNamespace(Stack stack, Service service) {
        return new StringBuilder().append(service.getName()).append(".").append(getStackNamespace(stack, service))
                .toString().toLowerCase();
    }

    public static String getStackNamespace(Stack stack, Service service) {
        return new StringBuilder().append(stack.getName()).append(".")
                .append(getGlobalNamespace(service)).toString().toLowerCase();
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
    
    public static UpgradedConfig mergeLaunchConfigs(Service service, Map<String, Object> newPrimaryLaunchConfig,
            List<Map<String, Object>> newSecondaryLaunchConfigs) {
        if (newPrimaryLaunchConfig == null && newSecondaryLaunchConfigs == null) {
            return null;
        }
        
        boolean changed = false;
        boolean isSelectorService = service.getSelectorContainer() == null;
        
        Map<String, Map<String, Object>> currentConfigsMap = getCurrentLaunchConfigs(service);

        // 1. Generate merge configs
        List<Map<String, Object>> mergedConfigs = new ArrayList<>();
        if (newPrimaryLaunchConfig != null) {
            mergedConfigs.add(newPrimaryLaunchConfig);
        }
        if (newSecondaryLaunchConfigs != null && !newSecondaryLaunchConfigs.isEmpty()) {
            for (Map<String, Object> newConfig : newSecondaryLaunchConfigs) {
                Object name = newConfig.get("name");
                Object imageUuid = newConfig.get(InstanceConstants.FIELD_IMAGE_UUID);
                if (isSelectorService && imageUuid != null
                        && StringUtils.equalsIgnoreCase(ServiceConstants.IMAGE_NONE, imageUuid.toString())) {
                    currentConfigsMap.remove(name);
                    changed = true;
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
            String name = mergedConfig.get("name") != null ? mergedConfig.get("name").toString() : service
                    .getName();
            try {
                Map<String, Object> currentConfig = currentConfigsMap.get(name);
                if (currentConfig == null) {
                    resetVersion = true;
                    continue;
                }
                currentVersion = String.valueOf(currentConfig.get(ServiceConstants.FIELD_VERSION));
                for (String key : mergedConfig.keySet()) {
                    if (currentConfig.containsKey(key)) {
                        if (key.equalsIgnoreCase(InstanceConstants.FIELD_PORTS)) {
                            preserveOldRandomPorts(service, mergedConfig, currentConfig);
                        }
                        // if field triggers upgrade + value changed, trigger upgrade
                        if (upgradeTriggerFields.contains(key)
                                && !currentConfig.get(key).equals(mergedConfig.get(key))) {
                            resetVersion = true;
                        }
                        currentConfig.remove(key);
                    } else if (upgradeTriggerFields.contains(key)) {
                        resetVersion = true;
                    }
                }

                // if there are no updatable keys left,
                // consider force upgrade
                boolean upgradableFieldsLeft = false;
                for (String key : currentConfig.keySet()) {
                    if (upgradeTriggerFields.contains(key)) {
                        upgradableFieldsLeft = true;
                        break;
                    }
                }
                if (!upgradableFieldsLeft) {
                    resetVersion = true;
                }
                mergedConfig.putAll(currentConfig);
                currentConfigsMap.remove(name);
            } finally {
                if (resetVersion) {
                    setVersion(mergedConfig, generatedVersion);
                    changed = true;
                } else {
                    setVersion(mergedConfig, currentVersion);
                }
            }
        }

        mergedConfigs.addAll(currentConfigsMap.values());

        return new UpgradedConfig(service, mergedConfigs, changed);
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

    public static class UpgradedConfig {
        Map<String, Object> primaryLaunchConfig;
        List<Map<String, Object>> secondaryLaunchConfigs;
        boolean runUpgrade;

        @SuppressWarnings("unchecked")
        public UpgradedConfig(Service service, List<Map<String, Object>> launchConfigs, boolean runUpgrade) {
            super();
            
            Map<String, Map<String, Object>> secondaryLCTemp = new HashMap<>();
            
            for (Map<String, Object> lc : launchConfigs) {
                if (!lc.containsKey("name")) {
                    this.primaryLaunchConfig = lc;
                } else {
                    secondaryLCTemp.put(lc.get("name").toString(), lc);
                }
            }
            
            // preserve the initial order of the secondary launch configs
            List<Object> secondary = DataAccessor.fields(service)
                    .withKey(ServiceConstants.FIELD_SECONDARY_LAUNCH_CONFIGS)
                    .withDefault(Collections.EMPTY_LIST).as(
                            List.class);
            List<String> secNames = new ArrayList<>();
            for (Object sec : secondary) {
                secNames.add(CollectionUtils.toMap(sec).get("name").toString());
            }
                    
            if (!secondaryLCTemp.isEmpty()) {
                secondaryLaunchConfigs = new ArrayList<>();
                for (String secName : secNames) {
                    if (secondaryLCTemp.containsKey(secName)) {
                        secondaryLaunchConfigs.add(secondaryLCTemp.get(secName));
                        secondaryLCTemp.remove(secName);
                    }
                }
                // add the rest
                secondaryLaunchConfigs.addAll(secondaryLCTemp.values());
            }

            this.runUpgrade = runUpgrade;
        }

        public Map<String, Object> getPrimaryLaunchConfig() {
            return primaryLaunchConfig;
        }

        public List<Map<String, Object>> getSecondaryLaunchConfigs() {
            return secondaryLaunchConfigs;
        }

        public boolean isRunUpgrade() {
            return runUpgrade;
        }
    }

    public static final InServiceUpgradeStrategy getStrategy(Service service, Pair<InstanceRevision, InstanceRevision> currentPreviousRevision,
            boolean upgrade) {
        boolean startFirst = DataAccessor.fieldBool(service, ServiceConstants.FIELD_START_FIRST);
        Long batchSize = DataAccessor.fieldLong(service, ServiceConstants.FIELD_BATCHSIZE);
        Long intervalMillis = DataAccessor.fieldLong(service, ServiceConstants.FIELD_INTERVAL_MILLISEC);

        if (currentPreviousRevision == null) {
            return null;
        }
        Map<String, Object> current = null;
        Map<String, Object> previous = null;

        if (upgrade) {
            current = CollectionUtils.toMap(DataAccessor.field(
                    currentPreviousRevision.getLeft(), InstanceConstants.FIELD_INSTANCE_SPECS, Object.class));
            previous = CollectionUtils.toMap(DataAccessor.field(
                    currentPreviousRevision.getRight(), InstanceConstants.FIELD_INSTANCE_SPECS, Object.class));
        } else {
            current = CollectionUtils.toMap(DataAccessor.field(
                    currentPreviousRevision.getRight(), InstanceConstants.FIELD_INSTANCE_SPECS, Object.class));
            previous = CollectionUtils.toMap(DataAccessor.field(
                    currentPreviousRevision.getLeft(), InstanceConstants.FIELD_INSTANCE_SPECS, Object.class));
        }

        List<Object> secondaryLaunchConfigs = new ArrayList<>();
        for (String name : current.keySet()) {
            if (name.equalsIgnoreCase(service.getName())) {
                continue;
            }
            secondaryLaunchConfigs.add(current.get(name));
        }

        List<Object> previousSecondaryLaunchConfigs = new ArrayList<>();
        for (String name : previous.keySet()) {
            if (name.equalsIgnoreCase(service.getName())) {
                continue;
            }
            previousSecondaryLaunchConfigs.add(previous.get(name));
        }
        return new InServiceUpgradeStrategy(current.get(service.getName()), secondaryLaunchConfigs,
                previous.get(service.getName()), previousSecondaryLaunchConfigs, startFirst,
                intervalMillis, batchSize);
    }
    
    public static final List<String> getServiceImages(Service service) {
        List<String> images = new ArrayList<>();
        for (String lcName : getServiceLaunchConfigNames(service)) {
            Object imageUUID = getLaunchConfigObject(service, lcName, InstanceConstants.FIELD_IMAGE_UUID);
            if (imageUUID == null) {
                continue;
            }
            images.add(imageUUID.toString());
        }
        return images;
    }

}
