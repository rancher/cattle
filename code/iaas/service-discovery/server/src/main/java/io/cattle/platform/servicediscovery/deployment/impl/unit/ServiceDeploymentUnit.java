package io.cattle.platform.servicediscovery.deployment.impl.unit;

import static io.cattle.platform.core.model.tables.ServiceIndexTable.*;
import static io.cattle.platform.core.model.tables.ServiceTable.*;
import static io.cattle.platform.core.model.tables.StackTable.*;
import static io.cattle.platform.core.model.tables.VolumeTable.*;
import static io.cattle.platform.core.model.tables.VolumeTemplateTable.*;
import io.cattle.platform.activity.ActivityLog;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.ExternalEventConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.constants.VolumeConstants;
import io.cattle.platform.core.model.DeploymentUnit;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.ServiceExposeMap;
import io.cattle.platform.core.model.ServiceIndex;
import io.cattle.platform.core.model.Stack;
import io.cattle.platform.core.model.Volume;
import io.cattle.platform.core.model.VolumeTemplate;
import io.cattle.platform.core.util.ServiceUtil;
import io.cattle.platform.core.util.SystemLabels;
import io.cattle.platform.docker.constants.DockerInstanceConstants;
import io.cattle.platform.engine.process.impl.ProcessCancelException;
import io.cattle.platform.lock.LockCallback;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.process.common.util.ProcessUtils;
import io.cattle.platform.servicediscovery.deployment.DeploymentUnitInstance;
import io.cattle.platform.servicediscovery.deployment.impl.instance.ServiceDeploymentUnitInstance;
import io.cattle.platform.servicediscovery.deployment.impl.lock.StackVolumeLock;
import io.cattle.platform.servicediscovery.deployment.impl.manager.DeploymentUnitManagerImpl.DeploymentUnitManagerContext;
import io.cattle.platform.util.exception.DeploymentUnitAllocateException;
import io.cattle.platform.util.type.CollectionUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;

public class ServiceDeploymentUnit extends AbstractDeploymentUnit {

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

    Service service;
    Stack stack;
    Map<String, String> labels = new HashMap<>();
    Map<String, ServiceIndex> launchConfigToServiceIndexes = new HashMap<>();

    @SuppressWarnings("unchecked")
    public ServiceDeploymentUnit(DeploymentUnitManagerContext context, DeploymentUnit unit) {
        super(context, unit);
        this.service = context.objectManager.findOne(Service.class, SERVICE.ID, unit.getServiceId());
        this.stack = context.objectManager.findOne(Stack.class, STACK.ID, service.getStackId());
        this.labels = DataAccessor.fields(unit).withKey(InstanceConstants.FIELD_LABELS)
                .withDefault(Collections.EMPTY_MAP).as(Map.class);
        this.launchConfigNames = ServiceUtil.getLaunchConfigNames(service);
        collectDeploymentUnitInstances();
        generateSidekickReferences();
    }

    public void generateSidekickReferences() {
        for (String launchConfigName : launchConfigNames) {
            for (String sidekick : getSidekickRefs(launchConfigName)) {
                List<String> usedBy = sidekickUsedByMap.get(sidekick);
                if (usedBy == null) {
                    usedBy = new ArrayList<>();
                }
                usedBy.add(launchConfigName);
                sidekickUsedByMap.put(sidekick, usedBy);
            }
        }
    }

    protected void addMissingInstance(String launchConfigName) {
        if (!launchConfigToInstance.containsKey(launchConfigName)) {
            Integer order = Integer.valueOf(this.unit.getServiceIndex());
            String instanceName = ServiceUtil.generateServiceInstanceName(stack,
                    service, launchConfigName, order);
            DeploymentUnitInstance deploymentUnitInstance = new ServiceDeploymentUnitInstance(context, service,
                    stack, instanceName, null, null, launchConfigName);
            addDeploymentInstance(launchConfigName, deploymentUnitInstance);
        }
    }

    protected Map<String, Object> populateDeployParams(DeploymentUnitInstance instance,
            List<Integer> volumesFromInstanceIds, Integer networkContainerId, List<String> namedVolumes,
            Map<String, Long> internalVolumes, ServiceIndex serviceIndex) {
        Map<String, Object> deployParams = new HashMap<>();
        Map<String, String> instanceLabels = getLabels(instance);
        deployParams.put(InstanceConstants.FIELD_LABELS, instanceLabels);
        if (volumesFromInstanceIds != null && !volumesFromInstanceIds.isEmpty()) {
            deployParams.put(DockerInstanceConstants.FIELD_VOLUMES_FROM, volumesFromInstanceIds);
        }
        Object hostId = instanceLabels.get(ServiceConstants.LABEL_SERVICE_REQUESTED_HOST_ID);
        if (hostId != null) {
            deployParams.put(InstanceConstants.FIELD_REQUESTED_HOST_ID, hostId);
        }

        if (networkContainerId != null) {
            deployParams.put(DockerInstanceConstants.FIELD_NETWORK_CONTAINER_ID, networkContainerId);
        }

        deployParams.put(InstanceConstants.FIELD_DEPLOYMENT_UNIT_UUID, unit.getUuid());
        deployParams.put(InstanceConstants.FIELD_DEPLOYMENT_UNIT_ID, unit.getId());
        deployParams.put(ServiceConstants.FIELD_VERSION, ServiceUtil.getLaunchConfigObject(
                service, instance.getLaunchConfigName(), ServiceConstants.FIELD_VERSION));
        addDns(instance, deployParams);

        deployParams.put(ServiceConstants.FIELD_INTERNAL_VOLUMES, namedVolumes);
        deployParams.put(InstanceConstants.FIELD_DATA_VOLUME_MOUNTS, internalVolumes);
        deployParams.put(InstanceConstants.FIELD_SERVICE_ID, service.getId());
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

    @Override
    protected void createImpl() {
        for (String launchConfigName : launchConfigNames) {
            createServiceIndex(launchConfigName);
        }
        for (String launchConfigName : launchConfigNames) {
            createInstance(launchConfigName);
        }
    }

    public DeploymentUnitInstance createInstance(String launchConfigName) {
        addMissingInstance(launchConfigName);
        List<Integer> volumesFromInstanceIds = getSidekickContainersId(launchConfigName, SidekickType.DATA);
        List<Integer> networkContainerIds = getSidekickContainersId(launchConfigName, SidekickType.NETWORK);
        Integer networkContainerId = networkContainerIds.isEmpty() ? null : networkContainerIds.get(0);
        List<String> namedVolumes = getNamedVolumes(launchConfigName);
        List<String> internalVolumes = new ArrayList<>();
        Map<String, Long> volumeMounts = new HashMap<>();
        for (String namedVolume : namedVolumes) {
            getOrCreateVolume(namedVolume, volumeMounts, internalVolumes);
        }
        launchConfigToInstance.get(launchConfigName)
                .create(
                        populateDeployParams(launchConfigToInstance.get(launchConfigName),
                                volumesFromInstanceIds,
                                networkContainerId, internalVolumes, volumeMounts,
                                launchConfigToServiceIndexes.get(launchConfigName)));

        DeploymentUnitInstance toReturn = launchConfigToInstance.get(launchConfigName);
        return toReturn;
    }
    
    @SuppressWarnings("unchecked")
    protected List<Integer> getSidekickContainersId(String launchConfigName, SidekickType sidekickType) {
        List<Integer> sidekickInstanceIds = new ArrayList<>();
        Object sidekickInstances = ServiceUtil.getLaunchConfigObject(service, launchConfigName,
                sidekickType.launchConfigFieldName);
        if (sidekickInstances != null) {
            if (sidekickType.isList) {
                sidekickInstanceIds.addAll((List<Integer>) sidekickInstances);
            } else {
                sidekickInstanceIds.add((Integer) sidekickInstances);
            }
        }

        Object sidekicksLaunchConfigObj = ServiceUtil.getLaunchConfigObject(service, launchConfigName,
                sidekickType.launchConfigType);
        if (sidekicksLaunchConfigObj != null) {
            List<String> sidekicksLaunchConfigNames = new ArrayList<>();
            if (sidekickType.isList) {
                sidekicksLaunchConfigNames.addAll((List<String>) sidekicksLaunchConfigObj);
            } else {
                sidekicksLaunchConfigNames.add(sidekicksLaunchConfigObj.toString());
            }
            for (String sidekickLaunchConfigName : sidekicksLaunchConfigNames) {
                // check if the service is present in the service map (it can be referenced, but removed already)
                if (sidekickLaunchConfigName.toString().equalsIgnoreCase(service.getName())) {
                    sidekickLaunchConfigName = ServiceConstants.PRIMARY_LAUNCH_CONFIG_NAME;
                }
                DeploymentUnitInstance sidekickUnitInstance = launchConfigToInstance.get(sidekickLaunchConfigName
                        .toString());
                if (sidekickUnitInstance == null) {
                    // request new instance creation
                    sidekickUnitInstance = createInstance(sidekickLaunchConfigName);
                }
                sidekickInstanceIds.add(sidekickUnitInstance.getInstance().getId()
                        .intValue());
            }
        }

        return sidekickInstanceIds;
    }

    protected void addDns(DeploymentUnitInstance instance, Map<String, Object> deployParams) {
        boolean addDns = true;
        Object labelsObj = ServiceUtil.getLaunchConfigObject(
                service, instance.getLaunchConfigName(), InstanceConstants.FIELD_LABELS);
        if (labelsObj != null) {
            Map<String, Object> labels = CollectionUtils.toMap(labelsObj);
            if (labels.containsKey(SystemLabels.LABEL_USE_RANCHER_DNS)
                    && !Boolean.valueOf(SystemLabels.LABEL_USE_RANCHER_DNS))
                addDns = false;
        }

        if (addDns) {
            deployParams.put(DockerInstanceConstants.FIELD_DNS_SEARCH, getSearchDomains());
        }
    }

    protected Map<String, String> getLabels(DeploymentUnitInstance instance) {
        Map<String, String> labels = new HashMap<>();
        String serviceName = service.getName();
        if (!ServiceConstants.PRIMARY_LAUNCH_CONFIG_NAME.equals(instance.getLaunchConfigName())) {
            serviceName = serviceName + '/' + instance.getLaunchConfigName();
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
        labels.put(ServiceConstants.LABEL_SERVICE_DEPLOYMENT_UNIT, getUuid());

        /*
         * Put label with launch config name
         */
        labels.put(ServiceConstants.LABEL_SERVICE_LAUNCH_CONFIG, instance.getLaunchConfigName());

        labels.putAll(this.labels);

        return labels;
    }

    protected List<String> getSearchDomains() {
        String stackNamespace = ServiceUtil.getStackNamespace(this.stack, this.service);
        String serviceNamespace = ServiceUtil
                .getServiceNamespace(this.stack, this.service);
        return Arrays.asList(stackNamespace, serviceNamespace);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected List<String> getSidekickRefs(String launchConfigName) {
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
            if (name.equalsIgnoreCase(service.getName())) {
                toReturn.add(ServiceConstants.PRIMARY_LAUNCH_CONFIG_NAME);
            } else {
                toReturn.add(name);
            }
        }
        return toReturn;
    }

    protected void getOrCreateVolume(String volumeName, Map<String, Long> volumeMounts,
            List<String> internalVolumes) {
        if (unit.getServiceIndex() == null) {
            return;
        }
        String[] splitted = volumeName.split(":");
        String volumeNamePostfix = splitted[0];
        String volumePath = volumeName.replaceFirst(splitted[0] + ":", "");

        final VolumeTemplate template = context.objectManager.findOne(VolumeTemplate.class, VOLUME_TEMPLATE.ACCOUNT_ID,
                service.getAccountId(), VOLUME_TEMPLATE.REMOVED, null, VOLUME_TEMPLATE.NAME, splitted[0],
                VOLUME_TEMPLATE.STACK_ID, stack.getId());
        if (template == null) {
            return;
        }

        Volume volume = null;
        if (template.getExternal()) {
            // external volume should exist, otherwise fail
            volume = context.objectManager.findAny(Volume.class, VOLUME.ACCOUNT_ID, service.getAccountId(),
                    VOLUME.REMOVED, null, VOLUME.NAME, template.getName());
            if (volume == null) {
                throw new DeploymentUnitAllocateException("Failed to locate volume for for deployment unit ["
                        + getUuid() + "]", null, this.unit);
            }
            return;
        } else {
            final String postfix = io.cattle.platform.util.resource.UUID.randomUUID().toString();
            if (template.getPerContainer()) {
                String name = stack.getName() + "_" + volumeNamePostfix + "_" + this.unit.getServiceIndex() + "_"
                        + getUuid() + "_";
                // append 5 random chars
                List<? extends Volume> volumes = context.objectManager
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
                    volume = createVolume(service, template, name + postfix.substring(0, 5));
                }
            } else {
                final String name = stack.getName() + "_" + volumeNamePostfix + "_";
                volume = context.lockMgr.lock(new StackVolumeLock(stack, name), new LockCallback<Volume>() {
                    @Override
                    public Volume doWithLock() {
                        Volume existing = null;
                        List<? extends Volume> volumes = context.objectManager
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
                        return createVolume(service, template, new String(name + postfix.substring(0, 5)));
                    }
                });
            }
        }
        volumeMounts.put(volumePath, volume.getId());
        internalVolumes.add(volumeName);
    }

    private Volume createVolume(Service service, VolumeTemplate template, String name) {
        Map<String, Object> params = new HashMap<>();
        if (template.getPerContainer()) {
            params.put(ServiceConstants.FIELD_DEPLOYMENT_UNIT_ID, unit.getId());
        }
        params.put("name", name);
        params.put("accountId", service.getAccountId());
        params.put(ServiceConstants.FIELD_STACK_ID, service.getStackId());
        params.put(ServiceConstants.FIELD_VOLUME_TEMPLATE_ID, template.getId());
        params.put(VolumeConstants.FIELD_VOLUME_DRIVER_OPTS,
                DataAccessor.fieldMap(template, VolumeConstants.FIELD_VOLUME_DRIVER_OPTS));
        params.put(VolumeConstants.FIELD_VOLUME_DRIVER, template.getDriver());
        return context.resourceDao.createAndSchedule(Volume.class, params);
    }

    @SuppressWarnings("unchecked")
    protected List<String> getNamedVolumes(String launchConfigName) {
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

    @SuppressWarnings("unchecked")
    protected void collectDeploymentUnitInstances() {
        List<Pair<Instance, ServiceExposeMap>> serviceInstances = context.exposeMapDao
                .listDeploymentUnitInstancesExposeMaps(service, unit);
        for (Pair<Instance, ServiceExposeMap> serviceInstance : serviceInstances) {
            Instance instance = serviceInstance.getLeft();
            ServiceExposeMap exposeMap = serviceInstance.getRight();
            Map<String, String> instanceLabels = DataAccessor.fields(instance)
                    .withKey(InstanceConstants.FIELD_LABELS).withDefault(Collections.EMPTY_MAP).as(Map.class);
            String launchConfigName = instanceLabels
                    .get(ServiceConstants.LABEL_SERVICE_LAUNCH_CONFIG);
            DeploymentUnitInstance unitInstance = new ServiceDeploymentUnitInstance(context, service,
                    stack, instance.getName(), instance, exposeMap, launchConfigName);
            addDeploymentInstance(launchConfigName, unitInstance);
        }
    }

    protected void removeAllDeploymentUnitInstances(String reason, String level) {
        /*
         * Delete all instances. This should be non-blocking (don't wait)
         */
        for (DeploymentUnitInstance instance : getDeploymentUnitInstances()) {
            instance.remove(reason, level);
        }
    }

    protected void cleanupVolumes(boolean force) {
        for (Volume volume : getVolumesToCleanup(force)) {
            try {
                context.objectProcessManager.scheduleStandardProcessAsync(StandardProcess.REMOVE, volume,
                        null);
            } catch (ProcessCancelException e) {
                context.objectProcessManager.scheduleStandardProcessAsync(StandardProcess.DEACTIVATE,
                        volume, ProcessUtils.chainInData(new HashMap<String, Object>(),
                                ExternalEventConstants.PROC_VOL_DEACTIVATE,
                                ExternalEventConstants.PROC_VOL_REMOVE));
            }
        }
    }

    protected List<Volume> getVolumesToCleanup(boolean force) {
        List<? extends Volume> volumes = new ArrayList<>();
        if (force) {
            volumes = context.objectManager.find(Volume.class, VOLUME.REMOVED, null,
                    VOLUME.DEPLOYMENT_UNIT_ID, unit.getId());
        } else {
            volumes = context.volumeDao.getVolumesOnRemovedAndInactiveHosts(unit.getId(), unit.getAccountId());
        }

        List<Volume> toCleanup = new ArrayList<>();

        for (Volume volume : volumes) {
            if (volume.getState().equals(CommonStatesConstants.REMOVED) || volume.getState().equals(
                    CommonStatesConstants.REMOVING)) {
                continue;
            }
            toCleanup.add(volume);
        }
        return toCleanup;
    }

    @Override
    public void remove(String reason, String level) {
        removeAllDeploymentUnitInstances(reason, level);
        cleanupVolumes(true);
        cleanupServiceIndexes();
    }

    protected void cleanupServiceIndexes() {
        for (ServiceIndex serviceIndex : context.objectManager.find(ServiceIndex.class, SERVICE_INDEX.SERVICE_ID,
                service.getId(), SERVICE_INDEX.REMOVED, null, SERVICE_INDEX.SERVICE_INDEX_, unit.getServiceIndex())) {
            if (serviceIndex.getState().equalsIgnoreCase(CommonStatesConstants.REMOVING)) {
                continue;
            }
            context.objectProcessManager.scheduleStandardProcessAsync(StandardProcess.REMOVE, serviceIndex, null);
        }
    }

    @Override
    public void cleanup(String reason, String level) {
        removeAllDeploymentUnitInstances(reason, level);
        cleanupVolumes(false);
    }

    protected void cleanupInstanceWithMissingDep(String launchConfigName) {
        List<String> usedInLaunchConfigs = sidekickUsedByMap.get(launchConfigName);
        if (usedInLaunchConfigs == null) {
            return;
        }
        for (String usedInLaunchConfig : usedInLaunchConfigs) {
            DeploymentUnitInstance usedByInstance = launchConfigToInstance.get(usedInLaunchConfig);
            if (usedByInstance == null) {
                continue;
            }
            removeDeploymentUnitInstance(usedByInstance, ServiceConstants.AUDIT_LOG_REMOVE_BAD, ActivityLog.INFO);
            cleanupInstanceWithMissingDep(usedInLaunchConfig);
        }
    }

    @Override
    protected void cleanupDependencies() {
        /*
         * Delete all the units having missing dependencies
         */
        for (String launchConfigName : launchConfigNames) {
            if (!launchConfigToInstance.containsKey(launchConfigName)) {
                cleanupInstanceWithMissingDep(launchConfigName);
            }
        }
    }

    @Override
    protected void cleanupUnhealthy() {
        for (DeploymentUnitInstance instance : this.getDeploymentUnitInstances()) {
            if (instance.isUnhealthy()) {
                removeDeploymentUnitInstance(instance, ServiceConstants.AUDIT_LOG_REMOVE_UNHEATLHY, ActivityLog.INFO);
            }
        }
    }

    public Service getService() {
        return service;
    }

    @Override
    public List<DeploymentUnitInstance> getInstancesWithMistmatchedIndexes() {
        List<DeploymentUnitInstance> toReturn = new ArrayList<>();
        for (DeploymentUnitInstance instance : this.getDeploymentUnitInstances()) {
            if (instance instanceof ServiceDeploymentUnitInstance) {
                ServiceDeploymentUnitInstance si = (ServiceDeploymentUnitInstance) instance;
                if (si.getInstance() == null) {
                    continue;
                }
                String index = ServiceConstants.getServiceSuffixFromInstanceName(si.getInstance().getName());
                if (!index.equalsIgnoreCase(unit.getServiceIndex())) {
                    toReturn.add(si);
                }
            }
        }
        return toReturn;
    }

    @SuppressWarnings("unchecked")
    protected void createServiceIndex(String launchConfigName) {
        // create index
        ServiceIndex serviceIndexObj = context.serviceDao.createServiceIndex(service, launchConfigName,
                unit.getServiceIndex());

        // allocate ip address if not set
        if (DataAccessor.fieldBool(service, ServiceConstants.FIELD_SERVICE_RETAIN_IP)) {
            Object requestedIpObj = ServiceUtil.getLaunchConfigObject(service, launchConfigName,
                    InstanceConstants.FIELD_REQUESTED_IP_ADDRESS);
            String requestedIp = null;
            if (requestedIpObj != null) {
                requestedIp = requestedIpObj.toString();
            } else {
                // can be passed via labels
                Object labels = ServiceUtil.getLaunchConfigObject(service, launchConfigName,
                        InstanceConstants.FIELD_LABELS);
                if (labels != null) {
                    requestedIp = ((Map<String, String>) labels).get(SystemLabels.LABEL_REQUESTED_IP);
                }
            }
            context.sdService.allocateIpToServiceIndex(service, serviceIndexObj, requestedIp);
        }
        launchConfigToServiceIndexes.put(launchConfigName, serviceIndexObj);
    }

}
