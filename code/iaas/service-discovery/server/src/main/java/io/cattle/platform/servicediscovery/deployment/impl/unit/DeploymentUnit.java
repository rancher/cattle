package io.cattle.platform.servicediscovery.deployment.impl.unit;

import static io.cattle.platform.core.model.tables.VolumeTable.*;
import static io.cattle.platform.core.model.tables.VolumeTemplateTable.*;
import io.cattle.platform.core.constants.AgentConstants;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.ExternalEventConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.constants.VolumeConstants;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.Stack;
import io.cattle.platform.core.model.Volume;
import io.cattle.platform.core.model.VolumeTemplate;
import io.cattle.platform.core.util.SystemLabels;
import io.cattle.platform.docker.constants.DockerInstanceConstants;
import io.cattle.platform.engine.process.impl.ProcessCancelException;
import io.cattle.platform.iaas.api.auditing.AuditEventType;
import io.cattle.platform.lock.LockCallback;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.object.util.TransitioningUtils;
import io.cattle.platform.process.common.util.ProcessUtils;
import io.cattle.platform.servicediscovery.api.util.ServiceDiscoveryUtil;
import io.cattle.platform.servicediscovery.deployment.DeploymentUnitInstance;
import io.cattle.platform.servicediscovery.deployment.DeploymentUnitInstanceIdGenerator;
import io.cattle.platform.servicediscovery.deployment.InstanceUnit;
import io.cattle.platform.servicediscovery.deployment.impl.DeploymentManagerImpl.DeploymentServiceContext;
import io.cattle.platform.servicediscovery.deployment.impl.lock.StackVolumeLock;
import io.cattle.platform.util.exception.ServiceInstanceAllocateException;
import io.cattle.platform.util.type.CollectionUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

public class DeploymentUnit {

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
    String uuid;
    DeploymentServiceContext context;
    Map<String, String> unitLabels = new HashMap<>();
    Map<String, DeploymentUnitInstance> launchConfigToInstance = new HashMap<>();
    List<String> launchConfigNames = new ArrayList<>();
    Map<String, List<String>> sidekickUsedByMap = new HashMap<>();
    io.cattle.platform.core.model.DeploymentUnit unit;

    private static List<String> supportedUnitLabels = Arrays
            .asList(ServiceConstants.LABEL_SERVICE_REQUESTED_HOST_ID);

    /*
     * This constructor is called to add existing unit
     */
    public DeploymentUnit(DeploymentServiceContext context, String uuid,
            Service service, List<DeploymentUnitInstance> deploymentUnitInstances, Map<String, String> labels,
            Stack stack,
            Map<String, io.cattle.platform.core.model.DeploymentUnit> uuidToUnit) {
        this(context, uuid, service, stack, uuidToUnit.get(uuid));
        for (DeploymentUnitInstance instance : deploymentUnitInstances) {
            addDeploymentInstance(instance.getLaunchConfigName(), instance);
        }
        setLabels(labels);
    }

    protected DeploymentUnit(DeploymentServiceContext context, String uuid, Service service, Stack stack,
            io.cattle.platform.core.model.DeploymentUnit unit) {
        this.context = context;
        this.service = service;
        this.uuid = uuid;
        this.unit = unit;
        this.stack = stack;
        this.launchConfigNames = ServiceDiscoveryUtil.getServiceLaunchConfigNames(service);
        for (String launchConfigName : launchConfigNames) {
            for (String sidekick : getSidekickRefs(service, launchConfigName)) {
                List<String> usedBy = sidekickUsedByMap.get(sidekick);
                if (usedBy == null) {
                    usedBy = new ArrayList<>();
                }
                usedBy.add(launchConfigName);
                sidekickUsedByMap.put(sidekick, usedBy);
            }
        }
    }


    /*
     * this constructor is called to create a new unit
     */
    public DeploymentUnit(DeploymentServiceContext context, Service service, Map<String, String> labels,
            DeploymentUnitInstanceIdGenerator svcInstanceIdGenerator, Stack stack) {
        this(context, io.cattle.platform.util.resource.UUID.randomUUID().toString(), service, stack, null);
        setLabels(labels);

        if (StringUtils.equalsIgnoreCase(ServiceConstants.SERVICE_INDEX_DU_STRATEGY,
                DataAccessor.fieldString(service, ServiceConstants.FIELD_SERVICE_INDEX_STRATEGY))) {
            Map<String, Object> params = new HashMap<>();
            // create deploymentunit
            params.put("uuid", this.uuid);
            params.put(ServiceConstants.FIELD_SERVICE_ID, service.getId());
            params.put(InstanceConstants.FIELD_SERVICE_INSTANCE_SERVICE_INDEX,
                    svcInstanceIdGenerator.getNextAvailableId());
            params.put("accountId", service.getAccountId());
            params.put(InstanceConstants.FIELD_LABELS, this.unitLabels);
            this.unit = context.objectManager.create(io.cattle.platform.core.model.DeploymentUnit.class, params);
        }

    }

    protected void setLabels(Map<String, String> labels) {
        if (labels != null) {
            for (String label : labels.keySet()) {
                if (supportedUnitLabels.contains(label)) {
                    this.unitLabels.put(label, labels.get(label));
                }
            }
        }
    }

    private void createMissingUnitInstances(DeploymentUnitInstanceIdGenerator svcInstanceIdGenerator) {
        Integer order = null;
        for (String launchConfigName : launchConfigNames) {
            if (!launchConfigToInstance.containsKey(launchConfigName)) {
                if (this.unit != null) {
                    order = Integer.valueOf(this.unit.getServiceIndex());
                } else if (order == null) {
                    order = svcInstanceIdGenerator.getNextAvailableId(launchConfigName);
                }
                String instanceName = ServiceDiscoveryUtil.generateServiceInstanceName(stack,
                        service, launchConfigName, order);
                DeploymentUnitInstance deploymentUnitInstance = context.deploymentUnitInstanceFactory
                        .createDeploymentUnitInstance(context, uuid, service, instanceName, null, launchConfigName);
                addDeploymentInstance(launchConfigName, deploymentUnitInstance);
            }
        }
    }

    public boolean isError() {
        /*
         * This should check for instances with an error transitioning state
         */
        for (DeploymentUnitInstance instance : getDeploymentUnitInstances()) {
            if (instance.isError()) {
                return true;
            }
        }
        return false;
    }

    private Host getDeploymentUnitHost() {
        for (DeploymentUnitInstance deployUnitInstance : getDeploymentUnitInstances()) {
            if (!(deployUnitInstance instanceof InstanceUnit)) {
                // external deployment units do not have instances
                return null;
            }

            Instance instance = ((InstanceUnit) deployUnitInstance).getInstance();
            if (instance != null && instance.getId() != null) {
                // TODO: Performance-wise, this is really bad! Especially, since we already
                // know what host is going down from the host trigger.

                // Check whether this instance has been deployed and if so, what is the state of the
                // host?
                Host host = context.exposeMapDao.getHostForInstance(instance.getId());
                if (host != null) {
                    return host;
                }
            }
        }
        return null;
    }

    private boolean isRemovedHost(Host host) {
        if (host == null) {
            return false;
        }
        return CommonStatesConstants.REMOVING.equals(host.getState()) ||
                    CommonStatesConstants.REMOVED.equals(host.getState()) ||
                    CommonStatesConstants.PURGING.equals(host.getState()) ||
                    CommonStatesConstants.PURGED.equals(host.getState());
    }

    private boolean isAgentDisconnected(Host host) {
        if (host == null) {
            return false;
        }
        Agent agent = context.objectManager.loadResource(Agent.class, host.getAgentId());
        if (agent != null && (AgentConstants.STATE_RECONNECTING.equals(agent.getState()) ||
                AgentConstants.STATE_DISCONNECTED.equals(agent.getState()) || AgentConstants.STATE_DISCONNECTING
                    .equals(agent.getState()))) {
            return true;
        }
        return false;
    }

    public void remove(String reason, String level) {
        /*
         * Delete all instances. This should be non-blocking (don't wait)
         */
        for (DeploymentUnitInstance instance : getDeploymentUnitInstances()) {
            String error = "";
            if (instance instanceof InstanceUnit) {
                error = TransitioningUtils.getTransitioningError(((DefaultDeploymentUnitInstance) instance).getInstance());
            }
            if (StringUtils.isNotBlank(error)) {
                reason = reason + ": " + error;
            }
            instance.generateAuditLog(AuditEventType.delete, reason, level);
            instance.remove();
        }

        if (unit != null) {
            clenaupVolumes();
            Map<String, Object> params = new HashMap<>();
            params.put(ObjectMetaDataManager.REMOVED_FIELD, new Date());
            params.put(ObjectMetaDataManager.REMOVE_TIME_FIELD, new Date());
            params.put(ObjectMetaDataManager.STATE_FIELD, CommonStatesConstants.REMOVED);
            context.objectManager.setFields(unit, params);
        }
    }

    public void clenaupVolumes() {
        if (unit == null) {
            return;
        }
        List<? extends Volume> volumes = context.objectManager.find(Volume.class, VOLUME.REMOVED, null,
                VOLUME.DEPLOYMENT_UNIT_ID, unit.getId());
        for (Volume volume : volumes) {
            if (!(volume.getState().equals(CommonStatesConstants.REMOVED) || volume.getState().equals(
                    CommonStatesConstants.REMOVING))) {
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
    }

    public void waitForRemoval() {
        for (DeploymentUnitInstance instance : getDeploymentUnitInstances()) {
            instance.waitForRemoval();
        }
    }

    public void cleanupUnit() {
        /*
         * Delete all the units having missing dependencies
         */
        cleanupInstancesWithMissingDependencies();
    }

    public void stop() {
        /*
         * stops all instances. This should be non-blocking (don't wait)
         */
        for (DeploymentUnitInstance instance : getDeploymentUnitInstances()) {
            instance.stop();
        }
    }

    public void start() {
        for (DeploymentUnitInstance instance : getDeploymentUnitInstances()) {
            instance.start();
        }
    }

    public void create(DeploymentUnitInstanceIdGenerator svcInstanceIdGenerator) {
        /*
         * Start the instances in the correct order depending on the volumes from.
         * Attempt to start things in parallel, but if not possible (like volumes-from) then start each service
         * sequentially.
         *
         * If there are three services but only two containers, create the third
         *
         * If one of the containers service health is bad, then create another one (but don't delete the existing).
         *
         */
        createMissingUnitInstances(svcInstanceIdGenerator);
        List<DeploymentUnitInstance> createdInstances = createServiceInstances();
        for (DeploymentUnitInstance instance : createdInstances) {
            instance.scheduleCreate();
        }
    }

    protected List<DeploymentUnitInstance> createServiceInstances() {
        List<DeploymentUnitInstance> createdInstances = new ArrayList<>();
        for (String launchConfigName : launchConfigNames) {
            createdInstances.add(createInstance(launchConfigName, service));
        }
        return createdInstances;
    }

    public void waitForStart(){
        // sort based on dependencies
        List<String> sortedLCs = new ArrayList<>();
        for (String lc : launchConfigToInstance.keySet()) {
            sortSidekicks(sortedLCs, lc);
        }

        List<DeploymentUnitInstance> sortedInstances = new ArrayList<>();
        for (String lc : sortedLCs) {
            sortedInstances.add(launchConfigToInstance.get(lc));
        }
        for (DeploymentUnitInstance instance : sortedInstances) {
            instance.waitForStart();
        }
    }

    protected void sortSidekicks(List<String> sorted, String lc) {
        List<String> sidekicks = getSidekickRefs(service, lc);
        for (String sidekick : sidekicks) {
            sortSidekicks(sorted, sidekick);
        }
        if (!sorted.contains(lc)) {
            sorted.add(lc);
        }
    }

    protected DeploymentUnitInstance createInstance(String launchConfigName, Service service) {
        List<Integer> volumesFromInstanceIds = getSidekickContainersId(service, launchConfigName, SidekickType.DATA);
        List<Integer> networkContainerIds = getSidekickContainersId(service, launchConfigName, SidekickType.NETWORK);
        Integer networkContainerId = networkContainerIds.isEmpty() ? null : networkContainerIds.get(0);
        List<String> namedVolumes = getNamedVolumes(launchConfigName, service);
        List<String> internalVolumes = new ArrayList<>();
        Map<String, Long> volumeMounts = new HashMap<>();
        for (String namedVolume : namedVolumes) {
            createVolume(service, namedVolume, volumeMounts, internalVolumes);
        }
        launchConfigToInstance.get(launchConfigName)
                .create(
                        populateDeployParams(launchConfigToInstance.get(launchConfigName),
                                volumesFromInstanceIds,
                                networkContainerId, internalVolumes, volumeMounts));

        DeploymentUnitInstance toReturn = launchConfigToInstance.get(launchConfigName);
        return toReturn;
    }

    protected void createVolume(final Service service, String volumeName, Map<String, Long> volumeMounts,
            List<String> internalVolumes) {
        if (this.unit == null) {
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
                throw new ServiceInstanceAllocateException("Failed to locate volume for instance of deployment unit ["
                        + uuid + "]", null, null);
            }
            return;
        } else {
            final String postfix = io.cattle.platform.util.resource.UUID.randomUUID().toString();
            if (template.getPerContainer()) {
                String name = stack.getName() + "_" + volumeNamePostfix + "_" + this.unit.getServiceIndex() + "_"
                        + uuid + "_";
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
    protected List<String> getNamedVolumes(String launchConfigName, Service service) {
        Object dataVolumesObj = ServiceDiscoveryUtil.getLaunchConfigObject(service, launchConfigName,
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
    protected List<Integer> getSidekickContainersId(Service service, String launchConfigName, SidekickType sidekickType) {
        List<Integer> sidekickInstanceIds = new ArrayList<>();
        Object sidekickInstances = ServiceDiscoveryUtil.getLaunchConfigObject(service, launchConfigName,
                sidekickType.launchConfigFieldName);
        if (sidekickInstances != null) {
            if (sidekickType.isList) {
                sidekickInstanceIds.addAll((List<Integer>) sidekickInstances);
            } else {
                sidekickInstanceIds.add((Integer) sidekickInstances);
            }
        }

        Object sidekicksLaunchConfigObj = ServiceDiscoveryUtil.getLaunchConfigObject(service, launchConfigName,
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
                if (sidekickUnitInstance != null && sidekickUnitInstance instanceof InstanceUnit) {
                    if (((InstanceUnit) sidekickUnitInstance).getInstance() == null) {
                        // request new instance creation
                        sidekickUnitInstance = createInstance(sidekickUnitInstance.getLaunchConfigName(), service);
                    }
                    sidekickInstanceIds.add(((InstanceUnit) sidekickUnitInstance).getInstance().getId()
                            .intValue());
                }
            }
        }

        return sidekickInstanceIds;
    }

    public boolean isStarted() {
        for (DeploymentUnitInstance instance : getDeploymentUnitInstances()) {
            if (!instance.isStarted()) {
                return false;
            }
        }
        return true;
    }

    public boolean isHealthCheckInitializing() {
        for (DeploymentUnitInstance instance : getDeploymentUnitInstances()) {
            if (instance.isHealthCheckInitializing()) {
                return true;
            }
        }
        return false;
    }

    public boolean isUnhealthy() {
        // returns list of instances that need cleanup (having bad health)
        for (DeploymentUnitInstance instance : getDeploymentUnitInstances()) {
            if (instance.isUnhealthy()) {
                return true;
            }
        }

        // unhealthy when host is removed
        Host host = getDeploymentUnitHost();
        if (host == null) {
            return false;
        }
        if (isRemovedHost(host)) {
            return true;
        }

        // unhealthy when in transitioning state on inactive host
        if (isTransitioning() && isAgentDisconnected(host)) {
            return true;
        }

        return false;
    }

    protected boolean isTransitioning() {
        for (DeploymentUnitInstance instance : getDeploymentUnitInstances()) {
            if (instance.isTransitioning()) {
                return true;
            }
        }
        return false;
    }

    protected Map<String, Object> populateDeployParams(DeploymentUnitInstance instance,
            List<Integer> volumesFromInstanceIds, Integer networkContainerId, List<String> namedVolumes, Map<String, Long> internalVolumes) {
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

        deployParams.put(InstanceConstants.FIELD_DEPLOYMENT_UNIT_UUID, this.uuid);
        deployParams.put(ServiceConstants.FIELD_VERSION, ServiceDiscoveryUtil.getLaunchConfigObject(
                instance.getService(), instance.getLaunchConfigName(), ServiceConstants.FIELD_VERSION));
        addDns(instance, deployParams);

        deployParams.put(ServiceConstants.FIELD_INTERNAL_VOLUMES, namedVolumes);
        deployParams.put(InstanceConstants.FIELD_DATA_VOLUME_MOUNTS, internalVolumes);

        return deployParams;
    }

    protected void addDns(DeploymentUnitInstance instance, Map<String, Object> deployParams) {
        boolean addDns = true;
        Object labelsObj = ServiceDiscoveryUtil.getLaunchConfigObject(
                instance.getService(), instance.getLaunchConfigName(), InstanceConstants.FIELD_LABELS);
        if (labelsObj != null) {
            Map<String, Object> labels = CollectionUtils.toMap(labelsObj);
            if (labels.containsKey(SystemLabels.LABEL_USE_RANCHER_DNS)
                    && !Boolean.valueOf(SystemLabels.LABEL_USE_RANCHER_DNS))
                addDns = false;
        }

        if (addDns) {
            deployParams.put(DockerInstanceConstants.FIELD_DNS_SEARCH, instance.getSearchDomains());
        }
    }

    protected Map<String, String> getLabels(DeploymentUnitInstance instance) {
        Map<String, String> labels = new HashMap<>();
        String serviceName = instance.getService().getName();
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
        labels.put(ServiceConstants.LABEL_SERVICE_DEPLOYMENT_UNIT, uuid);

        /*
         * Put label with launch config name
         */
        labels.put(ServiceConstants.LABEL_SERVICE_LAUNCH_CONFIG, instance.getLaunchConfigName());

        labels.putAll(this.unitLabels);

        return labels;
    }


    public Map<String, String> getLabels() {
        return unitLabels;
    }

    public List<DeploymentUnitInstance> getDeploymentUnitInstances() {
        List<DeploymentUnitInstance> instances = new ArrayList<>();
        instances.addAll(launchConfigToInstance.values());
        return instances;
    }

    public long getCreateIndex() {
        if (this.unit != null && getDeploymentUnitInstances().size() == 0) {
            return Long.valueOf(this.unit.getServiceIndex());
        }
        long createIndex = 0L;
        // find minimum created
        for (DeploymentUnitInstance i : getDeploymentUnitInstances()) {
            if (i.getCreateIndex() == null) {
                continue;
            }
            if (createIndex == 0) {
                createIndex = i.getCreateIndex();
                continue;
            }

            if (i.getCreateIndex().longValue() < createIndex) {
                createIndex = i.getCreateIndex();
            }
        }
        return createIndex;
    }

    public void addDeploymentInstance(String launchConfig, DeploymentUnitInstance instance) {
        this.launchConfigToInstance.put(launchConfig, instance);
    }

    public boolean isComplete() {
        return launchConfigNames.size() == launchConfigToInstance.size();
    }

    public void cleanupInstancesWithMissingDependencies() {
        for (String launchConfigName : launchConfigNames) {
            if (!launchConfigToInstance.containsKey(launchConfigName)) {
                cleanupInstanceWithMissingDep(launchConfigName);
            }
        }
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
            clenaupDeploymentInstance(usedByInstance);
            cleanupInstanceWithMissingDep(usedInLaunchConfig);
        }
    }

    protected void clenaupDeploymentInstance(DeploymentUnitInstance instance) {
        instance.remove();
        launchConfigToInstance.remove(instance.getLaunchConfigName());

    }

    @SuppressWarnings("unchecked")
    public List<String> getSidekickRefs(Service service, String launchConfigName) {
        List<String> configNames = new ArrayList<>();
        for (DeploymentUnit.SidekickType sidekickType : DeploymentUnit.SidekickType.supportedTypes) {
            Object sidekicksLaunchConfigObj = ServiceDiscoveryUtil.getLaunchConfigObject(service, launchConfigName,
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

}
