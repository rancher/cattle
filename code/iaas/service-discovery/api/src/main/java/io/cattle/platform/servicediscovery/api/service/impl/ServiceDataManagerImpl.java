package io.cattle.platform.servicediscovery.api.service.impl;

import static io.cattle.platform.core.model.tables.DeploymentUnitTable.*;
import static io.cattle.platform.core.model.tables.InstanceTable.*;
import static io.cattle.platform.core.model.tables.ServiceRevisionTable.*;
import static io.cattle.platform.core.model.tables.ServiceTable.*;
import static io.cattle.platform.core.model.tables.VolumeTable.*;
import static io.cattle.platform.core.model.tables.VolumeTemplateTable.*;
import io.cattle.platform.allocator.service.AllocationHelper;
import io.cattle.platform.configitem.version.ConfigItemStatusManager;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.constants.VolumeConstants;
import io.cattle.platform.core.dao.GenericResourceDao;
import io.cattle.platform.core.dao.InstanceDao;
import io.cattle.platform.core.dao.NetworkDao;
import io.cattle.platform.core.dao.ServiceConsumeMapDao;
import io.cattle.platform.core.dao.ServiceDao;
import io.cattle.platform.core.dao.ServiceExposeMapDao;
import io.cattle.platform.core.dao.impl.DefaultDeploymentUnitCreateLock;
import io.cattle.platform.core.dao.impl.ServiceCreateUpdateLock;
import io.cattle.platform.core.model.DeploymentUnit;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.ServiceExposeMap;
import io.cattle.platform.core.model.ServiceIndex;
import io.cattle.platform.core.model.ServiceRevision;
import io.cattle.platform.core.model.Stack;
import io.cattle.platform.core.model.Volume;
import io.cattle.platform.core.model.VolumeTemplate;
import io.cattle.platform.core.util.ServiceUtil;
import io.cattle.platform.core.util.SystemLabels;
import io.cattle.platform.docker.constants.DockerInstanceConstants;
import io.cattle.platform.eventing.EventService;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.lock.LockCallback;
import io.cattle.platform.lock.LockCallbackNoReturn;
import io.cattle.platform.lock.LockManager;
import io.cattle.platform.network.NetworkService;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.resource.pool.ResourcePoolManager;
import io.cattle.platform.servicediscovery.api.lock.StackVolumeLock;
import io.cattle.platform.servicediscovery.api.service.ServiceDataManager;
import io.cattle.platform.util.exception.DeploymentUnitAllocateException;
import io.cattle.platform.util.exception.ExecutionException;
import io.cattle.platform.util.type.CollectionUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

@Named
public class ServiceDataManagerImpl implements ServiceDataManager {
    @Inject
    ServiceConsumeMapDao consumeMapDao;
    @Inject
    ObjectManager objectManager;
    @Inject
    NetworkDao ntwkDao;
    @Inject
    ObjectProcessManager objectProcessManager;
    @Inject
    ServiceExposeMapDao exposeMapDao;
    @Inject
    JsonMapper jsonMapper;
    @Inject
    ResourcePoolManager poolManager;
    @Inject
    LockManager lockManager;
    @Inject
    AllocationHelper allocationHelper;
    @Inject
    EventService eventService;
    @Inject
    InstanceDao instanceDao;
    @Inject
    ConfigItemStatusManager itemManager;
    @Inject
    NetworkService networkService;
    @Inject
    ServiceDao serviceDao;
    @Inject
    GenericResourceDao resourceDao;

    public static class SidekickType {
        public static List<SidekickType> supportedTypes = new ArrayList<>();
        public static final SidekickType DATA = new SidekickType(DockerInstanceConstants.FIELD_VOLUMES_FROM,
                ServiceConstants.FIELD_DATA_VOLUMES_LAUNCH_CONFIG, true);
        public static final SidekickType NETWORK = new SidekickType(DockerInstanceConstants.FIELD_NETWORK_CONTAINER_ID,
                ServiceConstants.FIELD_NETWORK_LAUNCH_CONFIG, false);
        public String launchConfigFieldName;
        public String launchConfigType;
        public boolean isList;

        public SidekickType(String launchConfigFieldName, String launchConfigType, boolean isList) {
            this.launchConfigFieldName = launchConfigFieldName;
            this.launchConfigType = launchConfigType;
            this.isList = isList;
            supportedTypes.add(this);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> getDeploymentUnitInstanceData(Stack stack, Service service, DeploymentUnit unit,
            String launchConfigName, ServiceIndex serviceIndex, String instanceName) {
        Map<String, Object> deploymentUnitData = getDeploymentUnitData(stack, service, unit,
                launchConfigName, serviceIndex);
        addDns(stack, service, launchConfigName, deploymentUnitData);
        createNamedVolumes(stack, service, unit, launchConfigName, deploymentUnitData);
        Map<String, Object> deploymentUnitInstanceData = mergeWithServiceData(service,
                deploymentUnitData, launchConfigName);
        removeNamedVolumesFromLaunchConfig(deploymentUnitInstanceData,
                deploymentUnitInstanceData);

        deploymentUnitInstanceData.put("name", instanceName);
        Object labels = deploymentUnitInstanceData.get(InstanceConstants.FIELD_LABELS);
        if (labels != null) {
            String overrideHostName = ((Map<String, String>) labels)
                    .get(ServiceConstants.LABEL_OVERRIDE_HOSTNAME);
            if (StringUtils.equalsIgnoreCase(overrideHostName, "container_name")) {
                String domainName = (String) deploymentUnitInstanceData.get(DockerInstanceConstants.FIELD_DOMAIN_NAME);
                String overrideName = getOverrideHostName(domainName, instanceName);
                deploymentUnitInstanceData.put(InstanceConstants.FIELD_HOSTNAME, overrideName);
            }
        }

        return deploymentUnitInstanceData;
    }

    public void createNamedVolumes(Stack stack, Service service, DeploymentUnit unit, String launchConfigName,
            Map<String, Object> deploymentUnitParams) {
        List<String> namedVolumes = getNamedVolumes(service, launchConfigName);
        List<String> internalVolumes = new ArrayList<>();
        Map<String, Long> volumeMounts = new HashMap<>();
        Iterator<String> it = namedVolumes.iterator();
        while (it.hasNext()) {
            String namedVolume = it.next();
            getOrCreateVolume(stack, service, unit, namedVolume, volumeMounts, internalVolumes);
            if (!internalVolumes.contains(namedVolume)) {
                it.remove();
            }
        }

        deploymentUnitParams.put(ServiceConstants.FIELD_INTERNAL_VOLUMES, namedVolumes);
        deploymentUnitParams.put(InstanceConstants.FIELD_DATA_VOLUME_MOUNTS, volumeMounts);
    }

    private static String getOverrideHostName(String domainName, String instanceName) {
        String overrideName = instanceName;
        if (instanceName != null && instanceName.length() > 64) {
            // legacy code - to support old data where service suffix object wasn't created
            String serviceSuffix = ServiceConstants.getServiceIndexFromInstanceName(instanceName);
            String serviceSuffixWDivider = instanceName.substring(instanceName.lastIndexOf(serviceSuffix) - 1);
            int truncateIndex = 64 - serviceSuffixWDivider.length();
            if (domainName != null) {
                truncateIndex = truncateIndex - domainName.length() - 1;
            }
            overrideName = instanceName.substring(0, truncateIndex) + serviceSuffixWDivider;
        }
        return overrideName;
    }

    @SuppressWarnings("unchecked")
    protected Map<String, Object> getDeploymentUnitData(Stack stack, Service service, DeploymentUnit unit,
            String launchConfigName, ServiceIndex serviceIndex) {
        Map<String, Object> deployParams = new HashMap<>();
        Map<String, String> instanceLabels = generateDeploymentUnitLabels(service, stack, unit.getUuid(),
                launchConfigName);
        instanceLabels.putAll(DataAccessor.fields(unit).withKey(InstanceConstants.FIELD_LABELS)
                .withDefault(Collections.EMPTY_MAP).as(Map.class));
        deployParams.put(InstanceConstants.FIELD_LABELS, instanceLabels);

        Object hostId = instanceLabels.get(ServiceConstants.LABEL_SERVICE_REQUESTED_HOST_ID);
        if (hostId != null) {
            deployParams.put(InstanceConstants.FIELD_REQUESTED_HOST_ID, hostId);
        }

        deployParams.put(InstanceConstants.FIELD_DEPLOYMENT_UNIT_UUID, unit.getUuid());
        deployParams.put(InstanceConstants.FIELD_DEPLOYMENT_UNIT_ID, unit.getId());
        deployParams.put(ServiceConstants.FIELD_VERSION, ServiceUtil.getLaunchConfigObject(
                service, launchConfigName, ServiceConstants.FIELD_VERSION));

        if (service.getKind().equalsIgnoreCase(ServiceConstants.KIND_CONTAINER_SERVICE)) {
            deployParams.put(InstanceConstants.FIELD_CONTAINER_SERVICE_ID, service.getId());
        } else {
            deployParams.put(InstanceConstants.FIELD_SERVICE_ID, service.getId());
        }

        deployParams.put(InstanceConstants.FIELD_STACK_ID, service.getStackId());
        if (serviceIndex != null) {
            deployParams.put(InstanceConstants.FIELD_SERVICE_INSTANCE_SERVICE_INDEX_ID,
                    serviceIndex.getId());
            deployParams.put(InstanceConstants.FIELD_SERVICE_INSTANCE_SERVICE_INDEX,
                    serviceIndex.getServiceIndex());
            deployParams.put(InstanceConstants.FIELD_ALLOCATED_IP_ADDRESS, serviceIndex.getAddress());
        }
        return deployParams;
    }

    protected Map<String, String> generateDeploymentUnitLabels(Service service, Stack stack,
            String deploymentUnitUUID,
            String launchConfigName) {
        Map<String, String> labels = new HashMap<>();
        String serviceName = service.getName();
        if (!ServiceConstants.PRIMARY_LAUNCH_CONFIG_NAME.equals(launchConfigName)) {
            serviceName = serviceName + '/' + launchConfigName;
        }
        String envName = stack.getName();
        labels.put(ServiceConstants.LABEL_STACK_NAME, envName);
        labels.put(ServiceConstants.LABEL_STACK_SERVICE_NAME, envName + "/" + serviceName);

        // LEGACY: keeping backwards compatibility with 'project'
        labels.put(ServiceConstants.LABEL_PROJECT_NAME, envName);
        labels.put(ServiceConstants.LABEL_PROJECT_SERVICE_NAME, envName + "/" + serviceName);

        /*
         * Put label 'io.rancher.deployment.unit=this.uuid' on each one. This way
         * we can reference a set of containers later.
         */
        labels.put(ServiceConstants.LABEL_SERVICE_DEPLOYMENT_UNIT, deploymentUnitUUID);

        /*
         * 
         * Put label with launch config name
         */
        labels.put(ServiceConstants.LABEL_SERVICE_LAUNCH_CONFIG, launchConfigName);

        return labels;
    }

    protected void getOrCreateVolume(Stack stack, Service service, DeploymentUnit unit, String volumeName,
            Map<String, Long> volumeMounts,
            List<String> internalVolumes) {
        if (unit.getServiceIndex() == null) {
            return;
        }
        String[] splitted = volumeName.split(":");
        String volumeNamePostfix = splitted[0];
        String volumePath = volumeName.replaceFirst(splitted[0] + ":", "");

        final VolumeTemplate template = objectManager.findOne(VolumeTemplate.class, VOLUME_TEMPLATE.ACCOUNT_ID,
                service.getAccountId(), VOLUME_TEMPLATE.REMOVED, null, VOLUME_TEMPLATE.NAME, splitted[0],
                VOLUME_TEMPLATE.STACK_ID, stack.getId());
        if (template == null) {
            return;
        }

        Volume volume = null;
        if (template.getExternal()) {
            // external volume should exist, otherwise fail
            volume = objectManager.findAny(Volume.class, VOLUME.ACCOUNT_ID, service.getAccountId(),
                    VOLUME.REMOVED, null, VOLUME.NAME, template.getName());
            if (volume == null) {
                throw new DeploymentUnitAllocateException("Failed to locate volume for for deployment unit ["
                        + unit.getUuid() + "]", null, unit);
            }
            return;
        } else {
            final String postfix = io.cattle.platform.util.resource.UUID.randomUUID().toString();
            if (template.getPerContainer()) {
                String name = stack.getName() + "_" + volumeNamePostfix + "_" + unit.getServiceIndex() + "_"
                        + unit.getUuid() + "_";
                // append 5 random chars
                List<? extends Volume> volumes = objectManager
                        .find(Volume.class, VOLUME.ACCOUNT_ID, service.getAccountId(),
                                VOLUME.REMOVED, null, VOLUME.VOLUME_TEMPLATE_ID, template.getId(), VOLUME.STACK_ID,
                                stack.getId(), VOLUME.DEPLOYMENT_UNIT_ID, unit.getId());
                for (Volume vol : volumes) {
                    if (vol.getName().startsWith(name)) {
                        volume = vol;
                        break;
                    }
                }
                if (volume == null) {
                    volume = createVolume(service, unit, template, name + postfix.substring(0, 5));
                }
            } else {
                final String name = stack.getName() + "_" + volumeNamePostfix + "_";
                volume = lockManager.lock(new StackVolumeLock(stack, name), new LockCallback<Volume>() {
                    @Override
                    public Volume doWithLock() {
                        Volume existing = null;
                        List<? extends Volume> volumes = objectManager
                                .find(Volume.class, VOLUME.ACCOUNT_ID, service.getAccountId(),
                                        VOLUME.REMOVED, null, VOLUME.VOLUME_TEMPLATE_ID, template.getId(),
                                        VOLUME.STACK_ID,
                                        stack.getId());
                        for (Volume vol : volumes) {
                            if (vol.getName().startsWith(name)) {
                                existing = vol;
                                break;
                            }
                        }
                        if (existing != null) {
                            return existing;
                        }
                        return createVolume(service, unit, template, new String(name + postfix.substring(0, 5)));
                    }
                });
            }
        }
        volumeMounts.put(volumePath, volume.getId());
        internalVolumes.add(volumeName);
    }

    private Volume createVolume(Service service, DeploymentUnit unit, VolumeTemplate template, String name) {
        Map<String, Object> params = new HashMap<>();
        if (template.getPerContainer()) {
            params.put(ServiceConstants.FIELD_DEPLOYMENT_UNIT_ID, unit.getId());
        }
        params.put("name", name);
        params.put("accountId", service.getAccountId());
        params.put(InstanceConstants.FIELD_STACK_ID, service.getStackId());
        params.put(ServiceConstants.FIELD_VOLUME_TEMPLATE_ID, template.getId());
        params.put(VolumeConstants.FIELD_VOLUME_DRIVER_OPTS,
                DataAccessor.fieldMap(template, VolumeConstants.FIELD_VOLUME_DRIVER_OPTS));
        params.put(VolumeConstants.FIELD_VOLUME_DRIVER, template.getDriver());
        return resourceDao.createAndSchedule(Volume.class, params);
    }

    @SuppressWarnings("unchecked")
    protected List<String> getNamedVolumes(Service service, String launchConfigName) {
        Object dataVolumesObj = ServiceUtil.getLaunchConfigObject(service, launchConfigName,
                InstanceConstants.FIELD_DATA_VOLUMES);
        List<String> namedVolumes = new ArrayList<>();
        if (dataVolumesObj != null) {
            for (String volume : (List<String>) dataVolumesObj) {
                if (isNamedVolume(volume)) {
                    namedVolumes.add(volume);
                }
            }
        }
        return namedVolumes;
    }

    protected boolean isNamedVolume(String volumeName) {
        String[] splitted = volumeName.split(":");
        if (splitted.length < 2) {
            return false;
        }
        if (splitted[0].contains("/")) {
            return false;
        }
        return true;
    }

    protected void addDns(Stack stack, Service service, String launchConfigName, Map<String, Object> deployParams) {
        boolean addDns = true;
        Object labelsObj = ServiceUtil.getLaunchConfigObject(
                service, launchConfigName, InstanceConstants.FIELD_LABELS);
        if (labelsObj != null) {
            Map<String, Object> labels = CollectionUtils.toMap(labelsObj);
            if (labels.containsKey(SystemLabels.LABEL_USE_RANCHER_DNS)
                    && !Boolean.valueOf(SystemLabels.LABEL_USE_RANCHER_DNS))
                addDns = false;
        }

        if (addDns) {
            deployParams.put(DockerInstanceConstants.FIELD_DNS_SEARCH, getSearchDomains(stack, service));
        }
    }

    protected List<String> getSearchDomains(Stack stack, Service service) {
        String stackNamespace = ServiceUtil.getStackNamespace(stack, service);
        String serviceNamespace = ServiceUtil
                .getServiceNamespace(stack, service);
        return Arrays.asList(stackNamespace, serviceNamespace);
    }

    @SuppressWarnings("unchecked")
    private void removeNamedVolumesFromLaunchConfig(Map<String, Object> deploymentUnitParams,
            Map<String, Object> deploymentData) {
        // remove named volumes from the list
        if (deploymentData.get(InstanceConstants.FIELD_DATA_VOLUMES) != null) {
            List<String> dataVolumes = new ArrayList<>();
            dataVolumes.addAll((List<String>) deploymentData.get(InstanceConstants.FIELD_DATA_VOLUMES));
            if (deploymentUnitParams.get(ServiceConstants.FIELD_INTERNAL_VOLUMES) != null) {
                for (String namedVolume : (List<String>) deploymentUnitParams
                        .get(ServiceConstants.FIELD_INTERNAL_VOLUMES)) {
                    dataVolumes.remove(namedVolume);
                }
                deploymentData.put(InstanceConstants.FIELD_DATA_VOLUMES, dataVolumes);
            }
            deploymentData.remove(ServiceConstants.FIELD_INTERNAL_VOLUMES);
        }
    }

    @SuppressWarnings("unchecked")
    protected Map<String, Object> mergeWithServiceData(Service service, Map<String, Object> deployParams,
            String launchConfigName) {
        Map<String, Object> serviceData = ServiceUtil.getLaunchConfigDataAsMap(service, launchConfigName);
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
                        ServiceUtil.mergeLabels((Map<String, String>) launchConfigItems.get(key),
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

    @Override
    @SuppressWarnings("unchecked")
    public void joinService(Instance instance, Map<String, Object> originalConfig) {
        if (originalConfig.get(InstanceConstants.FIELD_LABELS) != null) {
            Map<String, String> labels = (Map<String, String>) originalConfig.get(InstanceConstants.FIELD_LABELS);
            if (labels.containsKey(SystemLabels.COMPOSE_SERVICE_LABEL)) {
                return;
            }
        }

        if (instance.getName() == null) {
            return;
        }

        if (!instance.getKind().equalsIgnoreCase(InstanceConstants.KIND_CONTAINER)) {
            return;
        }

        if (instance.getServiceId() != null) {
            return;
        }

        InstanceData data = getOrCreateInstanceData(instance);
        if (data == null) {
            throw new ExecutionException("Dependencies readiness error",
                    "Dependant instance hasn't gotten passed creating state");
        }
        Service service = data.getService();
        Stack stack = data.getStack();
        DeploymentUnit unit = data.getUnit();
        String launchConfig = data.getLaunchConfig();
        boolean updateUnit = false;
        if (instance.getDeploymentUnitId() == null) {
            updateInstance(instance, service, stack, unit, launchConfig);
            updateUnit = true;
        }

        updateService(originalConfig, data, service, unit);
        if (updateUnit) {
            objectProcessManager.scheduleStandardProcessAsync(StandardProcess.UPDATE, unit, null);
        }
    }

    public void updateService(Map<String, Object> originalConfig, InstanceData data, Service service,
            DeploymentUnit unit) {
        Map<String, Object> instanceLaunchConfig = new HashMap<>();
        try {
            String lc = jsonMapper.writeValueAsString(originalConfig);
            instanceLaunchConfig = jsonMapper.readValue(lc);
        } catch (IOException e) {
            return;
        }
        addLaunchConfigToService(service, instanceLaunchConfig, data.isPrimary());
    }

    @SuppressWarnings("unchecked")
    protected void updateInstance(Instance instance, Service service, Stack stack, DeploymentUnit unit,
            String launchConfig) {
        ServiceExposeMap exposeMap = exposeMapDao.createServiceInstanceMap(service, instance, true);
        if (exposeMap.getState().equalsIgnoreCase(CommonStatesConstants.REQUESTED)) {
            objectProcessManager.scheduleStandardChainedProcessAsync(StandardProcess.CREATE, StandardProcess.ACTIVATE,
                    service, null);
        }
        ServiceIndex serviceIndex = serviceDao.createServiceIndex(service, launchConfig, "0");

        Map<String, Object> data = getDeploymentUnitData(stack, service, unit,
                launchConfig, serviceIndex);
        Map<String, Object> labels = DataAccessor.fieldMap(instance, InstanceConstants.FIELD_LABELS);
        labels.putAll((Map<String, String>) data.get(InstanceConstants.FIELD_LABELS));
        data.put(InstanceConstants.FIELD_LABELS, labels);
        objectManager.setFields(instance, data);
    }

    protected InstanceData getOrCreateInstanceData(Instance instance) {
        DeploymentUnit du = null;
        Service service = null;
        List<Long> deps = InstanceConstants.getInstanceDependencies(instance);
        Stack stack = null;
        String launchConfigName = null;
        if (!deps.isEmpty()) {
            Instance depInstance = objectManager.findAny(Instance.class, INSTANCE.ID,
                    deps.get(0));
            if (depInstance.getDeploymentUnitId() == null) {
                return null;
            }
            du = objectManager.loadResource(DeploymentUnit.class, depInstance.getDeploymentUnitId());
            if (du == null) {
                return null;
            }
            service = objectManager.loadResource(Service.class, du.getServiceId());
            if (service == null) {
                return null;
            }
            stack = objectManager.loadResource(Stack.class, service.getStackId());
            if (stack == null) {
                return null;
            }
            launchConfigName = instance.getName();
        } else {
            stack = getOrCreateStack(instance);
            service = getOrCreateService(instance.getAccountId(), instance.getName(), stack.getId());
            du = getOrCreateDeploymentUnit(service);
            launchConfigName = ServiceConstants.PRIMARY_LAUNCH_CONFIG_NAME;
        }

        return new InstanceData(service, stack, launchConfigName, du);
    }

    public static class InstanceData {
        Service service;
        Stack stack;
        String launchConfig;
        DeploymentUnit unit;

        public InstanceData(Service service, Stack stack, String launchConfig, DeploymentUnit unit) {
            super();
            this.service = service;
            this.stack = stack;
            this.launchConfig = launchConfig;
            this.unit = unit;
        }

        public Service getService() {
            return service;
        }

        public Stack getStack() {
            return stack;
        }

        public String getLaunchConfig() {
            return launchConfig;
        }

        public DeploymentUnit getUnit() {
            return unit;
        }

        public boolean isPrimary() {
            return launchConfig.equalsIgnoreCase(ServiceConstants.PRIMARY_LAUNCH_CONFIG_NAME);
        }
    }

    public DeploymentUnit getOrCreateDeploymentUnit(Service service) {
        DeploymentUnit du = lockManager.lock(new DefaultDeploymentUnitCreateLock(service.getId()),
                new LockCallback<DeploymentUnit>() {
                    @Override
                    public DeploymentUnit doWithLock() {
                        DeploymentUnit du = objectManager.findAny(DeploymentUnit.class, DEPLOYMENT_UNIT.SERVICE_ID,
                                service.getId(), DEPLOYMENT_UNIT.SERVICE_INDEX,
                                "0", DEPLOYMENT_UNIT.REMOVED, null);
                        if (du == null) {
                            du = serviceDao.createDeploymentUnit(service.getAccountId(), service.getId(),
                                    service.getStackId(),
                                    null, 0);
                        }
                        return du;
                    }
                });

        if (du.getState().equalsIgnoreCase(CommonStatesConstants.REQUESTED)) {
            objectProcessManager.scheduleStandardChainedProcessAsync(StandardProcess.CREATE, StandardProcess.ACTIVATE,
                    du, null);
        }
        return du;
    }

    protected Stack getOrCreateStack(Instance instance) {
        Long stackId = instance.getStackId();
        if (stackId != null) {
            return objectManager.loadResource(Stack.class, stackId);
        }
        Stack stack = serviceDao.getOrCreateDefaultStack(instance.getAccountId());
        if (stack.getState().equalsIgnoreCase(CommonStatesConstants.REQUESTED)) {
            objectProcessManager.scheduleStandardProcessAsync(StandardProcess.CREATE,
                    stack, null);
        }
        return stack;
    }

    @SuppressWarnings("unchecked")
    protected void addLaunchConfigToService(Service service, Map<String, Object> instanceLaunchConfig, boolean isPrimary) {
        lockManager.lock(new ServiceCreateUpdateLock(service.getStackId(), service.getName()),
                new LockCallbackNoReturn() {
            @Override
            public void doWithLockNoResult() {
                        Map<String, Object> data = new HashMap<>();
                        List<String> lcs = getServiceLaunchConfigNames(service);
                        if (isPrimary) {
                            if (!lcs.contains(ServiceConstants.PRIMARY_LAUNCH_CONFIG_NAME)) {
                                data.put(ServiceConstants.FIELD_LAUNCH_CONFIG, instanceLaunchConfig);
                            }
                        } else {
                            if (lcs.contains(instanceLaunchConfig.get("name"))) {
                                return;
                            }
                            List<Object> secondaryLCs = DataAccessor.fields(service)
                                    .withKey(ServiceConstants.FIELD_SECONDARY_LAUNCH_CONFIGS)
                                    .withDefault(Collections.EMPTY_LIST).as(
                                            List.class);
                            modifyLaunchConfigData(instanceLaunchConfig);
                            secondaryLCs.add(instanceLaunchConfig);
                            data.put(ServiceConstants.FIELD_SECONDARY_LAUNCH_CONFIGS, secondaryLCs);
                        }
                        if (!data.isEmpty()) {
                            objectManager.setFields(objectManager.reload(service), data);
                        }
                        updateCurrentRevision(objectManager.reload(service));
            }
        });
    }

    @SuppressWarnings("unchecked")
    protected void removeLaunchConfigFromService(Service service, String launchConfigName) {
        lockManager.lock(new ServiceCreateUpdateLock(service.getStackId(), service.getName()),
                new LockCallbackNoReturn() {
                    @Override
                    public void doWithLockNoResult() {

                        boolean remove = false;
                        Map<String, List<String>> usedBy = getUsedBySidekicks(service);
                        for (String lc : usedBy.keySet()) {
                            if (usedBy.get(lc).contains(launchConfigName)) {
                                remove = true;
                                break;
                            }
                        }
                        if (!remove) {
                            return;
                        }
                        List<Map<String, Object>> secondaryLCs = DataAccessor.fields(service)
                                .withKey(ServiceConstants.FIELD_SECONDARY_LAUNCH_CONFIGS)
                                .withDefault(Collections.EMPTY_LIST).as(
                                        List.class);

                        List<Map<String, Object>> toUpdate = new ArrayList<>();
                        for (Map<String, Object> lc : secondaryLCs) {
                            if (launchConfigName.equalsIgnoreCase(lc.get("name").toString())) {
                                continue;
                            }
                            secondaryLCs.add(lc);
                        }

                        Map<String, Object> data = new HashMap<>();
                        data.put(ServiceConstants.FIELD_SECONDARY_LAUNCH_CONFIGS, toUpdate);
                        objectManager.setFields(objectManager.reload(service), data);
                        updateCurrentRevision(objectManager.reload(service));
            }
                });
    }

    public Service getOrCreateService(final long accountId, final String name, final long stackId) {
        Service service = lockManager.lock(new ServiceCreateUpdateLock(stackId, name), new LockCallback<Service>() {
            @Override
            public Service doWithLock() {
                Service service = objectManager.findAny(Service.class, SERVICE.STACK_ID, stackId, SERVICE.NAME,
                        name, SERVICE.REMOVED, null);
                if (service == null) {
                    Map<String, Object> data = new HashMap<>();
                    data.put(ObjectMetaDataManager.NAME_FIELD, name);
                    data.put(ObjectMetaDataManager.KIND_FIELD, ServiceConstants.KIND_CONTAINER_SERVICE);
                    data.put(ObjectMetaDataManager.ACCOUNT_FIELD, accountId);
                    data.put(InstanceConstants.FIELD_STACK_ID, stackId);
                    data.put(ServiceConstants.FIELD_SCALE, 1);
                    data.put(ServiceConstants.FIELD_BATCHSIZE, 1);
                    data.put(ServiceConstants.FIELD_INTERVAL_MILLISEC, 2000);
                    service = objectManager.create(Service.class, data);
                }
                return service;
            }
        });
        if (service.getState().equalsIgnoreCase(CommonStatesConstants.REQUESTED)) {
            objectProcessManager.scheduleStandardChainedProcessAsync(StandardProcess.CREATE, StandardProcess.ACTIVATE,
                    service, null);
        }
        return service;
    }

    @SuppressWarnings("unchecked")
    void modifyLaunchConfigData(Map<String, Object> launchConfigData) {
        if (launchConfigData.containsKey(DockerInstanceConstants.FIELD_NETWORK_CONTAINER_ID)) {
            Instance instance = objectManager.loadResource(Instance.class, Long.valueOf(launchConfigData.get(
                            DockerInstanceConstants.FIELD_NETWORK_CONTAINER_ID)
                            .toString()));
            if (instance != null) {
                launchConfigData.put(
                        ServiceConstants.FIELD_NETWORK_LAUNCH_CONFIG, instance.getName());
            }
            launchConfigData.remove(DockerInstanceConstants.FIELD_NETWORK_CONTAINER_ID);
        }

        Object volumesInstances = launchConfigData.get(DockerInstanceConstants.FIELD_VOLUMES_FROM);
        if (volumesInstances != null) {
            List<String> volumesFromLaunchConfigs = new ArrayList<>();
            for (Integer instanceId : (List<Integer>) volumesInstances) {
                Instance instance = objectManager.loadResource(Instance.class, instanceId.longValue());
                if (instance != null) {
                    volumesFromLaunchConfigs.add(instance.getName());
                }
            }
            launchConfigData.put(ServiceConstants.FIELD_DATA_VOLUMES_LAUNCH_CONFIG, volumesFromLaunchConfigs);
            launchConfigData.remove(DockerInstanceConstants.FIELD_VOLUMES_FROM);
        }
    }

    @Override
    public void leaveService(Instance instance) {
        if (instance.getContainerServiceId() == null) {
            return;
        }
        if (serviceDao.isServiceManagedInstance(instance)) {
            return;
        }
        boolean removedFromApi = InstanceConstants.ACTION_SOURCE_API.equalsIgnoreCase(DataAccessor.fieldString(
                instance, InstanceConstants.FIELD_REMOVE_SOURCE));
        if (!removedFromApi) {
            return;
        }

        Service service = objectManager.loadResource(Service.class, instance.getContainerServiceId());
        if (isPrimaryLaunchConfig(instance)) {
            if (!service.getState().equalsIgnoreCase(CommonStatesConstants.REMOVING)) {
                objectProcessManager.scheduleStandardProcessAsync(StandardProcess.REMOVE, service, null);
            }
            return;
        }
        removeLaunchConfigFromService(service, getLaunchConfigName(instance));
    }

    @SuppressWarnings("unchecked")
    boolean isPrimaryLaunchConfig(Instance instance) {
        Map<String, String> instanceLabels = DataAccessor.fields(instance)
                .withKey(InstanceConstants.FIELD_LABELS).withDefault(Collections.EMPTY_MAP).as(Map.class);
        String launchConfigName = instanceLabels
                .get(ServiceConstants.LABEL_SERVICE_LAUNCH_CONFIG);
        return ServiceConstants.PRIMARY_LAUNCH_CONFIG_NAME.equals(launchConfigName);
    }

    @SuppressWarnings("unchecked")
    String getLaunchConfigName(Instance instance) {
        Map<String, String> instanceLabels = DataAccessor.fields(instance)
                .withKey(InstanceConstants.FIELD_LABELS).withDefault(Collections.EMPTY_MAP).as(Map.class);
        return instanceLabels
                .get(ServiceConstants.LABEL_SERVICE_LAUNCH_CONFIG);
    }

    @Override
    public Map<String, List<String>> getUsedBySidekicks(Service service) {
        Map<String, List<String>> sidekickUsedByMap = new HashMap<>();
        for (String launchConfigName : ServiceUtil.getLaunchConfigNames(service)) {
            for (String sidekick : getLaunchConfigSidekickReferences(service, launchConfigName)) {
                List<String> usedBy = sidekickUsedByMap.get(sidekick);
                if (usedBy == null) {
                    usedBy = new ArrayList<>();
                }
                usedBy.add(launchConfigName);
                sidekickUsedByMap.put(sidekick, usedBy);
            }
        }
        return sidekickUsedByMap;
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<String> getLaunchConfigSidekickReferences(Service service, String launchConfigName) {
        List<String> configNames = new ArrayList<>();
        for (SidekickType sidekickType : SidekickType.supportedTypes) {
            Object sidekicksLaunchConfigObj = ServiceUtil.getLaunchConfigObject(service, launchConfigName,
                    sidekickType.launchConfigType);
            if (sidekicksLaunchConfigObj != null) {
                if (sidekickType.isList) {
                    configNames.addAll((List<String>) sidekicksLaunchConfigObj);
                } else {
                    configNames.add(sidekicksLaunchConfigObj.toString());
                }
            }
        }
        List<String> toReturn = new ArrayList<>();
        for (String name : configNames) {
            if (name == null) {
                continue;
            }
            if (name.equalsIgnoreCase(service.getName())) {
                toReturn.add(ServiceConstants.PRIMARY_LAUNCH_CONFIG_NAME);
            } else {
                toReturn.add(name);
            }
        }
        return toReturn;
    }

    @Override
    public ServiceRevision createRevision(Service service, Map<String, Object> primaryLaunchConfig,
            List<Map<String, Object>> secondaryLaunchConfigs, boolean isFirstRevision) {
        ServiceRevision revision = objectManager.findAny(ServiceRevision.class, SERVICE_REVISION.SERVICE_ID,
                service.getId(),
                SERVICE_REVISION.REMOVED, null);
        if ((revision != null && !isFirstRevision) || (revision == null && isFirstRevision)) {
            Map<String, Object> data = new HashMap<>();
            Map<String, Map<String, Object>> specs = generateRevisionSpecs(service, primaryLaunchConfig, secondaryLaunchConfigs);
            data.put(InstanceConstants.FIELD_INSTANCE_SPECS, specs);
            data.put(ObjectMetaDataManager.NAME_FIELD, service.getName());
            data.put(ObjectMetaDataManager.ACCOUNT_FIELD, service.getAccountId());
            data.put("serviceId", service.getId());
            revision = objectManager.create(ServiceRevision.class, data);
        }
        return revision;
    }

    public Map<String, Map<String, Object>> generateRevisionSpecs(Service service, Map<String, Object> primaryLaunchConfig,
            List<Map<String, Object>> secondaryLaunchConfigs) {
        Map<String, Map<String, Object>> specs = new HashMap<>();
        specs.put(service.getName(), primaryLaunchConfig);
        if (secondaryLaunchConfigs != null && !secondaryLaunchConfigs.isEmpty()) {
            for (Map<String, Object> spec : secondaryLaunchConfigs) {
                specs.put(spec.get("name").toString(), spec);
            }
        }
        return specs;
    }

    @Override
    public void cleanupServiceRevisions(Service service) {
        List<ServiceRevision> revisions = objectManager.find(ServiceRevision.class, SERVICE_REVISION.SERVICE_ID,
                service.getId(),
                SERVICE_REVISION.REMOVED, null);
        for (ServiceRevision revision : revisions) {
            Map<String, Object> params = new HashMap<>();
            params.put(ObjectMetaDataManager.REMOVED_FIELD, new Date());
            params.put(ObjectMetaDataManager.REMOVE_TIME_FIELD, new Date());
            params.put(ObjectMetaDataManager.STATE_FIELD, CommonStatesConstants.REMOVED);
            objectManager.setFields(revision, params);
        }
    }

    @Override
    public Pair<ServiceRevision, ServiceRevision> getCurrentAndPreviousRevisions(Service service) {
        ServiceRevision currentRevision = objectManager.findAny(ServiceRevision.class, SERVICE_REVISION.ID,
                service.getRevisionId());
        ServiceRevision previousRevision = objectManager.findAny(ServiceRevision.class, SERVICE_REVISION.ID,
                service.getPreviousRevisionId());
        return Pair.of(currentRevision, previousRevision);
    }

    @Override
    public ServiceRevision getCurrentRevision(Service service) {
        return objectManager.findAny(ServiceRevision.class, SERVICE_REVISION.ID,
                service.getRevisionId());
    }

    @Override
    public Pair<Map<String, Object>, List<Map<String, Object>>> getPrimaryAndSecondaryConfigFromRevision(
            ServiceRevision revision, Service service) {
        Map<String, Object> primary = new HashMap<>();
        List<Map<String, Object>> secondary = new ArrayList<>();
        Map<String, Map<String, Object>> specs = CollectionUtils.toMap(DataAccessor.field(
                revision, InstanceConstants.FIELD_INSTANCE_SPECS, Object.class));
        primary.putAll(specs.get(service.getName()));
        specs.remove(service.getName());
        secondary.addAll(specs.values());
        return Pair.of(primary, secondary);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void createInitialServiceRevision(Service service) {
        Map<String, Object> launchConfig = DataAccessor.fields(service)
                .withKey(ServiceConstants.FIELD_LAUNCH_CONFIG).withDefault(Collections.EMPTY_MAP)
                .as(Map.class);
        List<Map<String, Object>> secondaryLaunchConfigs = DataAccessor.fields(service)
                .withKey(ServiceConstants.FIELD_SECONDARY_LAUNCH_CONFIGS)
                .withDefault(Collections.EMPTY_LIST).as(
                        List.class);
        ServiceRevision revision = createRevision(service, launchConfig, secondaryLaunchConfigs, true);
        if (service.getRevisionId() == null) {
            Map<String, Object> data = new HashMap<>();
            data.put(InstanceConstants.FIELD_REVISION_ID, revision.getId());
            objectManager.setFields(service, data);
        }
    }

    @SuppressWarnings("unchecked")
    protected void updateCurrentRevision(Service service) {
        Map<String, Object> launchConfig = DataAccessor.fields(service)
                .withKey(ServiceConstants.FIELD_LAUNCH_CONFIG).withDefault(Collections.EMPTY_MAP)
                .as(Map.class);
        List<Map<String, Object>> secondaryLaunchConfigs = DataAccessor.fields(service)
                .withKey(ServiceConstants.FIELD_SECONDARY_LAUNCH_CONFIGS)
                .withDefault(Collections.EMPTY_LIST).as(
                        List.class);
        ServiceRevision revision = objectManager.findAny(ServiceRevision.class, SERVICE_REVISION.SERVICE_ID,
                service.getId(),
                SERVICE_REVISION.REMOVED, null);
        if (revision == null) {
            return;
        }
        Map<String, Object> data = new HashMap<>();
        Map<String, Map<String, Object>> specs = generateRevisionSpecs(service, launchConfig, secondaryLaunchConfigs);
        data.put(InstanceConstants.FIELD_INSTANCE_SPECS, specs);
        objectManager.setFields(revision, data);
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<String> getServiceLaunchConfigNames(Service service) {
        List<String> lcNames = new ArrayList<>();
        Map<String, Object> primaryLC = DataAccessor.fields(service)
                .withKey(ServiceConstants.FIELD_LAUNCH_CONFIG).withDefault(Collections.EMPTY_MAP)
                .as(Map.class);
        if (!primaryLC.isEmpty()) {
            lcNames.add(ServiceConstants.PRIMARY_LAUNCH_CONFIG_NAME);
        }
        List<Object> secondaryLCs = DataAccessor.fields(service)
                .withKey(ServiceConstants.FIELD_SECONDARY_LAUNCH_CONFIGS)
                .withDefault(Collections.EMPTY_LIST).as(
                        List.class);
        for (Object secondaryLaunchConfigObject : secondaryLCs) {
            Map<String, Object> lc = CollectionUtils.toMap(secondaryLaunchConfigObject);
            lcNames.add(lc.get("name").toString());
        }
        return lcNames;
    }
}
