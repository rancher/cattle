package io.cattle.platform.servicediscovery.deployment.impl.unit;

import static io.cattle.platform.core.model.tables.DeploymentUnitTable.*;
import static io.cattle.platform.core.model.tables.StackTable.*;
import static io.cattle.platform.core.model.tables.VolumeTable.*;
import static io.cattle.platform.core.model.tables.VolumeTemplateTable.*;
import io.cattle.platform.allocator.constraint.AffinityConstraintDefinition.AffinityOps;
import io.cattle.platform.allocator.constraint.ContainerLabelAffinityConstraint;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.VolumeConstants;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.Stack;
import io.cattle.platform.core.model.Volume;
import io.cattle.platform.core.model.VolumeTemplate;
import io.cattle.platform.core.util.SystemLabels;
import io.cattle.platform.docker.constants.DockerInstanceConstants;
import io.cattle.platform.iaas.api.auditing.AuditEventType;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.object.util.TransitioningUtils;
import io.cattle.platform.servicediscovery.api.constants.ServiceDiscoveryConstants;
import io.cattle.platform.servicediscovery.api.util.ServiceDiscoveryUtil;
import io.cattle.platform.servicediscovery.deployment.DeploymentUnitInstance;
import io.cattle.platform.servicediscovery.deployment.DeploymentUnitInstanceIdGenerator;
import io.cattle.platform.servicediscovery.deployment.InstanceUnit;
import io.cattle.platform.servicediscovery.deployment.impl.DeploymentManagerImpl.DeploymentServiceContext;
import io.cattle.platform.util.exception.ServiceInstanceAllocateException;
import io.cattle.platform.util.type.CollectionUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;

public class DeploymentUnit {

    public static class SidekickType {
        public static List<SidekickType> supportedTypes = new ArrayList<>();
        public static final SidekickType DATA = new SidekickType(DockerInstanceConstants.FIELD_VOLUMES_FROM,
                ServiceDiscoveryConstants.FIELD_DATA_VOLUMES_LAUNCH_CONFIG, true);
        public static final SidekickType NETWORK = new SidekickType(DockerInstanceConstants.FIELD_NETWORK_CONTAINER_ID,
                ServiceDiscoveryConstants.FIELD_NETWORK_LAUNCH_CONFIG, false);
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
    Stack env;
    String uuid;
    DeploymentServiceContext context;
    Map<String, String> unitLabels = new HashMap<>();
    Map<String, DeploymentUnitInstance> launchConfigToInstance = new HashMap<>();
    List<String> launchConfigNames = new ArrayList<>();
    Map<String, List<String>> sidekickUsedByMap = new HashMap<>();
    io.cattle.platform.core.model.DeploymentUnit unit;

    private static List<String> supportedUnitLabels = Arrays
            .asList(ServiceDiscoveryConstants.LABEL_SERVICE_REQUESTED_HOST_ID);

    /*
     * This constructor is called to add existing unit
     */
    public DeploymentUnit(DeploymentServiceContext context, String uuid,
            Service service, List<DeploymentUnitInstance> deploymentUnitInstances, Map<String, String> labels) {
        this(context, uuid, service);
        for (DeploymentUnitInstance instance : deploymentUnitInstances) {
            addDeploymentInstance(instance.getLaunchConfigName(), instance);
        }
        setLabels(labels);
    }

    protected DeploymentUnit(DeploymentServiceContext context, String uuid, Service service) {
        this.context = context;
        this.service = service;
        this.uuid = uuid;
        this.unit = context.objectManager.findOne(io.cattle.platform.core.model.DeploymentUnit.class,
                DEPLOYMENT_UNIT.ACCOUNT_ID,
                service.getAccountId(), DEPLOYMENT_UNIT.REMOVED, null, DEPLOYMENT_UNIT.SERVICE_ID, service.getId(),
                DEPLOYMENT_UNIT.UUID, uuid);
        this.env = context.objectManager.findOne(Stack.class, STACK.ID, service.getStackId());
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

    public Integer getServiceIndex() {
        List<? extends io.cattle.platform.core.model.DeploymentUnit> dus = context.objectManager.find(
                io.cattle.platform.core.model.DeploymentUnit.class,
                DEPLOYMENT_UNIT.ACCOUNT_ID,
                service.getAccountId(), DEPLOYMENT_UNIT.REMOVED, null, DEPLOYMENT_UNIT.SERVICE_ID, service.getId());
        List<Integer> indexes = new ArrayList<>();
        for (io.cattle.platform.core.model.DeploymentUnit du : dus) {
            indexes.add(Integer.valueOf(du.getServiceIndex()));
        }
        return DeploymentUnitInstanceIdGeneratorImpl.generateNewId(indexes);
    }

    /*
     * this constructor is called to create a new unit
     */
    public DeploymentUnit(DeploymentServiceContext context, Service service, Map<String, String> labels) {
        this(context, UUID.randomUUID().toString(), service);
        setLabels(labels);

        if (StringUtils.equalsIgnoreCase(ServiceDiscoveryConstants.SERVICE_INDEX_DU_STRATEGY,
                DataAccessor.fieldString(service, ServiceDiscoveryConstants.FIELD_SERVICE_INDEX_STRATEGY))) {
            Map<String, Object> params = new HashMap<>();
            // create deploymentunit
            params.put("uuid", this.uuid);
            params.put(ServiceDiscoveryConstants.FIELD_SERVICE_ID, service.getId());
            params.put(InstanceConstants.FIELD_SERVICE_INSTANCE_SERVICE_INDEX, getServiceIndex());
            params.put("accountId", service.getAccountId());
            params.put(InstanceConstants.FIELD_LABELS, this.unitLabels);
            this.unit = context.objectManager.create(io.cattle.platform.core.model.DeploymentUnit.class, params);
            context.objectProcessManager.scheduleStandardProcessAsync(StandardProcess.CREATE, this.unit, null);
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
                String instanceName = ServiceDiscoveryUtil.generateServiceInstanceName(env,
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

    private boolean isHostActive() {
        for (DeploymentUnitInstance deployUnitInstance : getDeploymentUnitInstances()) {
            if (!(deployUnitInstance instanceof InstanceUnit)) {
                // external deployment units do not have instances
                return true;
            }

            Instance instance = ((InstanceUnit)deployUnitInstance).getInstance();
            if (instance != null && instance.getId() != null) {
                // TODO: Performance-wise, this is really bad!  Especially, since we already
                // know what host is going down from the host trigger.

                // Check whether this instance has been deployed and if so, what is the state of the
                // host?
                Host host = context.exposeMapDao.getHostForInstance(instance.getId());
                if (host != null) {
                    if (CommonStatesConstants.REMOVING.equals(host.getState()) ||
                            CommonStatesConstants.REMOVED.equals(host.getState()) ||
                            CommonStatesConstants.PURGING.equals(host.getState()) ||
                            CommonStatesConstants.PURGED.equals(host.getState())) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public void remove(boolean waitForRemoval, String reason, String level) {
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

        if (waitForRemoval) {
            waitForRemoval();
        }

        // remove deployment unit object
        if (unit != null) {
            context.objectProcessManager.scheduleStandardProcessAsync(StandardProcess.REMOVE, unit, null);
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
        for (DeploymentUnitInstance instance : getDeploymentUnitInstances()) {
            instance.waitForStart();
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

    protected void createVolume(Service service, String volumeName, Map<String, Long> volumeMounts, List<String> internalVolumes) {
        String[] splitted = volumeName.split(":");
        String volumeNamePostfix = splitted[0];
        String volumePath = volumeName.replaceFirst(splitted[0] + ":", "");

        VolumeTemplate template = context.objectManager.findOne(VolumeTemplate.class, VOLUME_TEMPLATE.ACCOUNT_ID,
                service.getAccountId(), VOLUME_TEMPLATE.REMOVED, null, VOLUME_TEMPLATE.NAME, splitted[0],
                VOLUME_TEMPLATE.STACK_ID, env.getId());
        if (template == null) {
            return;
        }
        
        Volume volume = null;
        if (template.getExternal()) {
            // external volume should exist, otherwise fail
            volume = context.objectManager.findOne(Volume.class, VOLUME.ACCOUNT_ID, service.getAccountId(),
                    VOLUME.REMOVED, null, VOLUME.VOLUME_TEMPLATE_ID, template.getId(), VOLUME.STACK_ID, env.getId());
            if (volume == null) {
                throw new ServiceInstanceAllocateException("Failed to locate volume for instance of deployment unit ["
                        + uuid + "]", null, null);
            }
            return;
        } else {
            Map<String, Object> params = new HashMap<>();
            String name = "";
            if (template.getContainer()) {
                name = uuid + "_" + volumeNamePostfix;
                volume = context.objectManager
                        .findOne(Volume.class, VOLUME.ACCOUNT_ID, service.getAccountId(),
                                VOLUME.REMOVED, null, VOLUME.VOLUME_TEMPLATE_ID, template.getId(), VOLUME.STACK_ID,
                                env.getId(), VOLUME.NAME, name, VOLUME.DEPLOYMENT_UNIT_ID, unit.getId());
                params.put(ServiceDiscoveryConstants.FIELD_DEPLOYMENT_UNIT_ID, unit.getId());
            } else {
                name = env.getName() + "_" + volumeNamePostfix;
                volume = context.objectManager
                        .findOne(Volume.class, VOLUME.ACCOUNT_ID, service.getAccountId(),
                                VOLUME.REMOVED, null, VOLUME.VOLUME_TEMPLATE_ID, template.getId(), VOLUME.STACK_ID,
                                env.getId(), VOLUME.NAME, name);
            }

            if (volume == null) {
                params.put("name", name);
                params.put("accountId", service.getAccountId());
                params.put(ServiceDiscoveryConstants.FIELD_STACK_ID, service.getStackId());
                params.put(ServiceDiscoveryConstants.FIELD_VOLUME_TEMPLATE_ID, template.getId());
                params.put(VolumeConstants.FIELD_VOLUME_DRIVER_OPTS,
                        DataAccessor.fieldMap(template, VolumeConstants.FIELD_VOLUME_DRIVER_OPTS));
                params.put(VolumeConstants.FIELD_VOLUME_DRIVER, template.getDriver());
                volume = context.objectManager.create(Volume.class, params);
            }
            if (volume.getState().equalsIgnoreCase(CommonStatesConstants.REQUESTED)) {
                context.objectProcessManager.scheduleStandardProcessAsync(StandardProcess.CREATE, volume, null);
            }
        }
        volumeMounts.put(volumePath, volume.getId());
        internalVolumes.add(volumeName);
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
                    sidekickLaunchConfigName = ServiceDiscoveryConstants.PRIMARY_LAUNCH_CONFIG_NAME;
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
        if (!isHostActive()) {
            return true;
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
        Object hostId = instanceLabels.get(ServiceDiscoveryConstants.LABEL_SERVICE_REQUESTED_HOST_ID);
        if (hostId != null) {
            deployParams.put(InstanceConstants.FIELD_REQUESTED_HOST_ID, hostId);
        }

        if (networkContainerId != null) {
            deployParams.put(DockerInstanceConstants.FIELD_NETWORK_CONTAINER_ID, networkContainerId);
        }

        deployParams.put(InstanceConstants.FIELD_DEPLOYMENT_UNIT_UUID, this.uuid);
        deployParams.put(ServiceDiscoveryConstants.FIELD_VERSION, ServiceDiscoveryUtil.getLaunchConfigObject(
                instance.getService(), instance.getLaunchConfigName(), ServiceDiscoveryConstants.FIELD_VERSION));
        addDns(instance, deployParams);

        deployParams.put(ServiceDiscoveryConstants.FIELD_INTERNAL_VOLUMES, namedVolumes);
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
        if (!ServiceDiscoveryConstants.PRIMARY_LAUNCH_CONFIG_NAME.equals(instance.getLaunchConfigName())) {
            serviceName = serviceName + '/' + instance.getLaunchConfigName();
        }
        String envName = context.objectManager.loadResource(Stack.class, instance.getService().getStackId())
                .getName();
        labels.put(ServiceDiscoveryConstants.LABEL_STACK_NAME, envName);
        labels.put(ServiceDiscoveryConstants.LABEL_STACK_SERVICE_NAME, envName + "/" + serviceName);

        // LEGACY: keeping backwards compatibility with 'project'
        labels.put(ServiceDiscoveryConstants.LABEL_PROJECT_NAME, envName);
        labels.put(ServiceDiscoveryConstants.LABEL_PROJECT_SERVICE_NAME, envName + "/" + serviceName);

        /*
         * Put label 'io.rancher.deployment.unit=this.uuid' on each one. This way
         * we can reference a set of containers later.
         */
        labels.put(ServiceDiscoveryConstants.LABEL_SERVICE_DEPLOYMENT_UNIT, uuid);

        /*
         * Put label with launch config name
         */
        labels.put(ServiceDiscoveryConstants.LABEL_SERVICE_LAUNCH_CONFIG, instance.getLaunchConfigName());

        if (this.hasSidekicks()) {
            /*
             * Put affinity constraint on every instance to let allocator know that they should go to the same host
             */
            // TODO: Might change labels into a Multimap or add a service function to handle merging
            String containerLabelSoftAffinityKey = ContainerLabelAffinityConstraint.LABEL_HEADER_AFFINITY_CONTAINER_LABEL
                    + AffinityOps.SOFT_EQ.getLabelSymbol();
            labels.put(containerLabelSoftAffinityKey, ServiceDiscoveryConstants.LABEL_SERVICE_DEPLOYMENT_UNIT + "="
                    + this.uuid);
        }

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

    private boolean hasSidekicks() {
        return launchConfigNames.size() > 1;
    }

    public long getCreateIndex() {
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
                toReturn.add(ServiceDiscoveryConstants.PRIMARY_LAUNCH_CONFIG_NAME);
            } else {
                toReturn.add(name);
            }
        }

        return toReturn;
    }
}
