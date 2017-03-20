package io.cattle.platform.servicediscovery.api.service.impl;

import static io.cattle.platform.core.model.tables.DeploymentUnitTable.*;
import static io.cattle.platform.core.model.tables.InstanceTable.*;
import static io.cattle.platform.core.model.tables.ServiceRevisionTable.*;
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
import io.cattle.platform.engine.process.impl.ProcessCancelException;
import io.cattle.platform.eventing.EventService;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.lock.LockCallback;
import io.cattle.platform.lock.LockManager;
import io.cattle.platform.network.NetworkService;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.resource.pool.ResourcePoolManager;
import io.cattle.platform.servicediscovery.api.lock.ConvertToServiceLock;
import io.cattle.platform.servicediscovery.api.lock.StackVolumeLock;
import io.cattle.platform.servicediscovery.api.service.ServiceDataManager;
import io.cattle.platform.util.exception.DeploymentUnitAllocateException;
import io.cattle.platform.util.type.CollectionUtils;
import io.github.ibuildthecloud.gdapi.validation.ValidationErrorCodes;

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

    protected Map<String, Object> getDeploymentUnitData(Stack stack, Service service, DeploymentUnit unit,
            String launchConfigName, ServiceIndex serviceIndex) {
        Map<String, Object> deployParams = new HashMap<>();
        deployParams.put(InstanceConstants.FIELD_DEPLOYMENT_UNIT_UUID, unit.getUuid());
        deployParams.put(InstanceConstants.FIELD_DEPLOYMENT_UNIT_ID, unit.getId());
        deployParams.put(InstanceConstants.FIELD_STACK_ID, stack.getId());

        if (service != null) {
            deployParams.put(InstanceConstants.FIELD_SERVICE_ID, service.getId());
            Map<String, String> labels = generateDeploymentUnitLabels(service, stack, unit,
                    launchConfigName);

            deployParams.put(InstanceConstants.FIELD_LABELS, labels);

            Object hostId = labels.get(ServiceConstants.LABEL_SERVICE_REQUESTED_HOST_ID);
            if (hostId != null) {
                deployParams.put(InstanceConstants.FIELD_REQUESTED_HOST_ID, hostId);
            }
            deployParams.put(ServiceConstants.FIELD_VERSION, ServiceUtil.getLaunchConfigObject(
                    service, launchConfigName, ServiceConstants.FIELD_VERSION));
        }
        if (serviceIndex != null) {
            deployParams.put(InstanceConstants.FIELD_SERVICE_INSTANCE_SERVICE_INDEX_ID,
                    serviceIndex.getId());
            deployParams.put(InstanceConstants.FIELD_SERVICE_INSTANCE_SERVICE_INDEX,
                    serviceIndex.getServiceIndex());
            deployParams.put(InstanceConstants.FIELD_ALLOCATED_IP_ADDRESS, serviceIndex.getAddress());
        }
        return deployParams;
    }

    @SuppressWarnings("unchecked")
    protected Map<String, String> generateDeploymentUnitLabels(Service service, Stack stack,
            DeploymentUnit unit,
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
        labels.put(ServiceConstants.LABEL_SERVICE_DEPLOYMENT_UNIT, unit.getUuid());

        /*
         * 
         * Put label with launch config name
         */
        labels.put(ServiceConstants.LABEL_SERVICE_LAUNCH_CONFIG, launchConfigName);

        labels.putAll(DataAccessor.fields(unit).withKey(InstanceConstants.FIELD_LABELS)
                .withDefault(Collections.EMPTY_MAP).as(Map.class));

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
        String stackNamespace = ServiceUtil.getStackNamespace(stack);
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
    public void joinDeploymentUnit(Instance instance) {
        if (instance.getName() == null) {
            return;
        }

        if (instance.getNativeContainer()) {
            return;
        }

        if (serviceDao.isServiceManagedInstance(instance)) {
            return;
        }

        if (!instance.getKind().equalsIgnoreCase(InstanceConstants.KIND_CONTAINER)) {
            return;
        }
        
        if (instance.getStackId() == null) {
            return;
        }

        Map<String, Object> originalData = instanceDao.getRevisionConfig(instance);
        
        if (originalData.isEmpty()) {
            return;
        }

        DeploymentUnit unit = null;
        if (instance.getDeploymentUnitId() != null) {
            unit = objectManager.loadResource(DeploymentUnit.class, instance.getDeploymentUnitId());
        } else {
            List<Long> deps = InstanceConstants.getInstanceDependencies(instance);
            if (!deps.isEmpty()) {
                Instance depInstance = objectManager.findAny(Instance.class, INSTANCE.ID,
                        deps.get(0));
                unit = objectManager.loadResource(DeploymentUnit.class, depInstance.getDeploymentUnitId());
            }
        }

        if (unit == null) {
            unit = createDeploymentUnit(instance);
        }
        updateInstance(instance, unit);
    }

    protected void updateInstance(Instance instance, DeploymentUnit unit) {
        Stack stack = objectManager.loadResource(Stack.class, instance.getStackId());
        Map<String, Object> data = getDeploymentUnitData(stack, null, unit,
                instance.getName(), null);
        data.put(InstanceConstants.FIELD_SERVICE_INSTANCE_SERVICE_INDEX,
                STANDALONE_UNIT_INDEX);
        objectManager.setFields(instance, data);
    }

    public DeploymentUnit createDeploymentUnit(Instance instance) {
        DeploymentUnit du = serviceDao.createDeploymentUnit(instance.getAccountId(), null, instance.getStackId(), null,
                STANDALONE_UNIT_INDEX);
        if (du.getState().equalsIgnoreCase(CommonStatesConstants.REQUESTED)) {
            objectProcessManager.scheduleStandardChainedProcessAsync(StandardProcess.CREATE, StandardProcess.ACTIVATE,
                    du, null);
        }
        return du;
    }

    @Override
    public void leaveDeploymentUnit(Instance instance) {
        if (instance.getDeploymentUnitId() == null) {
            return;
        }
        if (serviceDao.isServiceManagedInstance(instance)) {
            return;
        }

        List<String> errorStates = Arrays.asList(InstanceConstants.STATE_ERRORING, InstanceConstants.STATE_ERROR);
        if (errorStates.contains(instance.getState())) {
            Map<String, Object> data = new HashMap<>();
            data.put(InstanceConstants.FIELD_DEPLOYMENT_UNIT_UUID, null);
            data.put(InstanceConstants.FIELD_DEPLOYMENT_UNIT_ID, null);
            objectManager.setFields(instance, data);
            return;
        }

        List<? extends Instance> instances = objectManager.find(Instance.class, INSTANCE.DEPLOYMENT_UNIT_ID,
                instance.getDeploymentUnitId(), INSTANCE.REMOVED, null);
        boolean remove = true;
        for (Instance i : instances) {
            // do not remove if there are instances assigned to unit
            List<String> removedStates = Arrays.asList(CommonStatesConstants.REMOVING, CommonStatesConstants.REMOVED);
            if (!removedStates.contains(i.getState())) {
                remove = false;
            }
        }
        
        if (remove) {
            DeploymentUnit unit = objectManager.loadResource(DeploymentUnit.class, instance.getDeploymentUnitId());
            if (unit == null || unit.getRemoved() != null
                    || unit.getState().equalsIgnoreCase(CommonStatesConstants.REMOVING)) {
                return;
            }
            try {
                objectProcessManager.scheduleStandardProcessAsync(StandardProcess.REMOVE, unit, null);
            } catch (ProcessCancelException e) {
                objectProcessManager.scheduleStandardChainedProcessAsync(StandardProcess.DEACTIVATE,
                        StandardProcess.REMOVE,
                        unit, null);
            }
        }
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
            Map<String, Map<String, Object>> configs = generateRevisionConfigs(service, primaryLaunchConfig, secondaryLaunchConfigs);
            data.put(ServiceConstants.FIELD_SERVICE_REVISION_CONFIGS, configs);
            data.put(ObjectMetaDataManager.NAME_FIELD, service.getName());
            data.put(ObjectMetaDataManager.ACCOUNT_FIELD, service.getAccountId());
            data.put("serviceId", service.getId());
            revision = objectManager.create(ServiceRevision.class, data);
        }
        return revision;
    }

    public Map<String, Map<String, Object>> generateRevisionConfigs(Service service, Map<String, Object> primaryLaunchConfig,
            List<Map<String, Object>> secondaryLaunchConfigs) {
        Map<String, Map<String, Object>> configs = new HashMap<>();
        configs.put(service.getName(), primaryLaunchConfig);
        if (secondaryLaunchConfigs != null && !secondaryLaunchConfigs.isEmpty()) {
            for (Map<String, Object> spec : secondaryLaunchConfigs) {
                configs.put(spec.get("name").toString(), spec);
            }
        }
        return configs;
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
        Map<String, Map<String, Object>> configs = CollectionUtils.toMap(DataAccessor.field(
                revision, ServiceConstants.FIELD_SERVICE_REVISION_CONFIGS, Object.class));
        primary.putAll(configs.get(service.getName()));
        configs.remove(service.getName());
        secondary.addAll(configs.values());
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
        Map<String, Map<String, Object>> configs = generateRevisionConfigs(service, launchConfig, secondaryLaunchConfigs);
        data.put(ServiceConstants.FIELD_SERVICE_REVISION_CONFIGS, configs);
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

    @Override
    public Object convertToService(Instance instance, String serviceName, long stackId) {
        List<? extends Instance> instances = objectManager.find(Instance.class, INSTANCE.DEPLOYMENT_UNIT_ID,
                instance.getDeploymentUnitId());
        List<Instance> secondary = new ArrayList<>();
        for (Instance sec : instances) {
            if (sec.getId().equals(instance.getId())) {
                continue;
            }
            if (!instance.getStackId().equals(stackId)) {
                ValidationErrorCodes.throwValidationError(ValidationErrorCodes.INVALID_OPTION,
                        "Sidekick container uuid " + instance.getUuid()
                                + " belongs to a different stack, can't convert");
            }
            if (sec.getName() == null) {
                ValidationErrorCodes.throwValidationError(ValidationErrorCodes.INVALID_OPTION,
                        "Sidekick container uuid " + instance.getUuid() + " is missing name");
            }
            secondary.add(sec);
        }
        Stack stack = objectManager.loadResource(Stack.class, stackId);
        return convertToServiceImpl(serviceName, stack, instance, secondary);
    }

    protected Service convertToServiceImpl(String serviceName, Stack stack, Instance primary, List<Instance> secondary) {
        DeploymentUnit du = objectManager.loadResource(DeploymentUnit.class, primary.getDeploymentUnitId());
        return lockManager.lock(new ConvertToServiceLock(du), new LockCallback<Service>() {
            @Override
            public Service doWithLock() {
                Service service = createService(serviceName, stack, primary, secondary);
                convertDeploymentUnitToService(du, service);
                convertInstancesToService(stack, service, du, primary, secondary);
                return service;
            }
        });
    }

    public void convertInstancesToService(Stack stack, Service service, DeploymentUnit unit, Instance primaryInstance,
            List<Instance> secondaryInstances) {
        List<Instance> instances = new ArrayList<>();
        instances.add(primaryInstance);
        instances.addAll(secondaryInstances);
        for (Instance instance : instances) {
            String launchConfig = instance.getId().equals(primaryInstance.getId()) ? ServiceConstants.PRIMARY_LAUNCH_CONFIG_NAME
                    : instance.getName();
            Map<String, String> unitLables = generateDeploymentUnitLabels(service, stack, unit,
                    launchConfig);
            Map<String, Object> instanceLabels = DataAccessor.fieldMap(instance, InstanceConstants.FIELD_LABELS);
            instanceLabels.putAll(unitLables);
            objectManager.setFields(instance, INSTANCE.SERVICE_ID, service.getId(), INSTANCE.STACK_ID, stack.getId(),
                    InstanceConstants.FIELD_LABELS, instanceLabels);
            ServiceExposeMap exposeMap = exposeMapDao.createServiceInstanceMap(service, instance, true, null);
            objectProcessManager.scheduleStandardProcessAsync(StandardProcess.CREATE, exposeMap, null);
        }
    }

    public void convertDeploymentUnitToService(DeploymentUnit du, Service service) {
        objectManager.setFields(du, DEPLOYMENT_UNIT.SERVICE_ID, service.getId());
    }

    public Service createService(String name, Stack stack, Instance primary, List<Instance> secondary) {
        Map<String, Object> data = new HashMap<>();
        data.put(ObjectMetaDataManager.NAME_FIELD, name);
        data.put(ObjectMetaDataManager.KIND_FIELD, ServiceConstants.KIND_SERVICE);
        data.put(ObjectMetaDataManager.ACCOUNT_FIELD, primary.getAccountId());
        data.put(InstanceConstants.FIELD_STACK_ID, stack.getId());

        Map<Long, String> containerIdToLaunchConfigName = new HashMap<>();
        containerIdToLaunchConfigName.put(primary.getId(), name);
        for (Instance sec : secondary) {
            containerIdToLaunchConfigName.put(sec.getId(), sec.getName());
        }

        Map<String, Object> primaryLC = getLaunchConfig(primary, containerIdToLaunchConfigName, true);
        List<Map<String, Object>> secondaryLCs = new ArrayList<>();
        for (Instance sec : secondary) {
            secondaryLCs.add(getLaunchConfig(sec, containerIdToLaunchConfigName, false));
        }

        data.put(ServiceConstants.FIELD_LAUNCH_CONFIG, primaryLC);
        data.put(ServiceConstants.FIELD_SECONDARY_LAUNCH_CONFIGS, secondaryLCs);
        data.put(ServiceConstants.FIELD_SCALE, 1);
        data.put(ServiceConstants.FIELD_BATCHSIZE, 1);
        data.put(ServiceConstants.FIELD_INTERVAL_MILLISEC, 2000);

        Service service = objectManager.create(Service.class, data);

        objectProcessManager.scheduleStandardChainedProcessAsync(StandardProcess.CREATE,
                StandardProcess.ACTIVATE,
                service, null);
        return objectManager.reload(service);
    }

    @SuppressWarnings("unchecked")
    Map<String, Object> getLaunchConfig(Instance instance, Map<Long, String> containerToLCName, boolean isPrimary) {
        Map<String, Object> config = instanceDao.getRevisionConfig(instance);
        if (isPrimary) {
            config.remove(ObjectMetaDataManager.NAME_FIELD);
        } else {
            config.put(ObjectMetaDataManager.NAME_FIELD, instance.getName());
        }
        if (config.containsKey(DockerInstanceConstants.FIELD_NETWORK_CONTAINER_ID)) {
            config.put(ServiceConstants.FIELD_NETWORK_LAUNCH_CONFIG,
                    containerToLCName.get(Long.valueOf(config.get(DockerInstanceConstants.FIELD_NETWORK_CONTAINER_ID)
                            .toString())));
            config.remove(DockerInstanceConstants.FIELD_NETWORK_CONTAINER_ID);
        }

        Object volumesInstances = config.get(DockerInstanceConstants.FIELD_VOLUMES_FROM);
        if (volumesInstances != null) {
            List<String> volumesFromLaunchConfigs = new ArrayList<>();
            for (Integer instanceId : (List<Integer>) volumesInstances) {
                volumesFromLaunchConfigs.add(containerToLCName.get(instanceId.longValue()));
            }
            config.put(ServiceConstants.FIELD_DATA_VOLUMES_LAUNCH_CONFIG, volumesFromLaunchConfigs);
            config.remove(DockerInstanceConstants.FIELD_VOLUMES_FROM);
        }

        return config;
    }
}
