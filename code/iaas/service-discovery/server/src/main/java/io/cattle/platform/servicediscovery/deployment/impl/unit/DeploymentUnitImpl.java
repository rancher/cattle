package io.cattle.platform.servicediscovery.deployment.impl.unit;

import static io.cattle.platform.core.model.tables.ServiceIndexTable.*;
import static io.cattle.platform.core.model.tables.ServiceTable.*;
import static io.cattle.platform.core.model.tables.StackTable.*;
import static io.cattle.platform.core.model.tables.VolumeTable.*;
import io.cattle.platform.activity.ActivityLog;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.ExternalEventConstants;
import io.cattle.platform.core.constants.HealthcheckConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.model.DeploymentUnit;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.ServiceExposeMap;
import io.cattle.platform.core.model.ServiceIndex;
import io.cattle.platform.core.model.Stack;
import io.cattle.platform.core.model.Volume;
import io.cattle.platform.core.util.ServiceUtil;
import io.cattle.platform.core.util.SystemLabels;
import io.cattle.platform.docker.constants.DockerInstanceConstants;
import io.cattle.platform.engine.process.impl.ProcessCancelException;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.process.common.util.ProcessUtils;
import io.cattle.platform.servicediscovery.api.service.impl.ServiceDataManagerImpl;
import io.cattle.platform.servicediscovery.api.service.impl.ServiceDataManagerImpl.SidekickType;
import io.cattle.platform.servicediscovery.deployment.DeploymentUnitInstance;
import io.cattle.platform.servicediscovery.deployment.impl.instance.DeploymentUnitInstanceImpl;
import io.cattle.platform.servicediscovery.deployment.impl.manager.DeploymentUnitManagerImpl.DeploymentUnitManagerContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;

public class DeploymentUnitImpl implements io.cattle.platform.servicediscovery.deployment.DeploymentUnit {

    Service service;
    Stack stack;
    Map<String, ServiceIndex> launchConfigToServiceIndexes = new HashMap<>();
    DeploymentUnitManagerContext context;
    Map<String, DeploymentUnitInstance> launchConfigToInstance = new HashMap<>();
    List<String> launchConfigNames = new ArrayList<>();
    Map<String, List<String>> sidekickUsedByMap = new HashMap<>();
    DeploymentUnit unit;

    public DeploymentUnitImpl(DeploymentUnit unit, DeploymentUnitManagerContext context) {
        this.context = context;
        this.unit = unit;
        this.service = context.objectManager.findOne(Service.class, SERVICE.ID, unit.getServiceId());
        this.stack = context.objectManager.findOne(Stack.class, STACK.ID, service.getStackId());
        this.launchConfigNames = context.svcDataMgr.getServiceLaunchConfigNames(service);
        collectDeploymentUnitInstances();
        generateSidekickReferences();
    }

    public void generateSidekickReferences() {
        sidekickUsedByMap.putAll(context.svcDataMgr.getUsedBySidekicks(service));
    }

    protected void addMissingInstance(String launchConfigName) {
        if (!launchConfigToInstance.containsKey(launchConfigName)) {
            String instanceName = getInstanceName(launchConfigName);
            DeploymentUnitInstance deploymentUnitInstance = new DeploymentUnitInstanceImpl(context, service,
                    stack, instanceName, null, null, launchConfigName);
            addDeploymentInstance(launchConfigName, deploymentUnitInstance);
        }
    }

    public String getInstanceName(String launchConfigName) {
        Integer order = Integer.valueOf(this.unit.getServiceIndex());
        String instanceName = ServiceUtil.generateServiceInstanceName(stack,
                service, launchConfigName, order);
        return instanceName;
    }

    protected Map<String, Object> populateDeployParams(String launchConfigName,
            ServiceIndex serviceIndex, List<Integer> volumesFromInstanceIds, Integer networkContainerId) {
        Map<String, Object> deployParams = context.svcDataMgr.getDeploymentUnitInstanceData(stack, service, unit,
                launchConfigName,
                serviceIndex,
                getInstanceName(launchConfigName));

        if (volumesFromInstanceIds != null && !volumesFromInstanceIds.isEmpty()) {
            deployParams.put(DockerInstanceConstants.FIELD_VOLUMES_FROM, volumesFromInstanceIds);
        }

        if (networkContainerId != null) {
            deployParams.put(DockerInstanceConstants.FIELD_NETWORK_CONTAINER_ID, networkContainerId);
        }

        return deployParams;
    }

    protected void createImpl() {
        for (String launchConfigName : launchConfigNames) {
            createServiceIndex(launchConfigName);
        }
        for (String launchConfigName : launchConfigNames) {
            getOrCreateInstance(launchConfigName);
        }
    }

    protected DeploymentUnitInstance getOrCreateInstance(String launchConfigName) {
        addMissingInstance(launchConfigName);
        List<Integer> volumesFromInstanceIds = getSidekickContainersId(launchConfigName,
                ServiceDataManagerImpl.SidekickType.DATA);
        List<Integer> networkContainerIds = getSidekickContainersId(launchConfigName, SidekickType.NETWORK);
        Integer networkContainerId = networkContainerIds.isEmpty() ? null : networkContainerIds.get(0);

        Map<String, Object> deployParams = populateDeployParams(launchConfigName,
                launchConfigToServiceIndexes.get(launchConfigName),
                volumesFromInstanceIds, networkContainerId);
        launchConfigToInstance.get(launchConfigName)
                .create(deployParams);

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
                    sidekickUnitInstance = getOrCreateInstance(sidekickLaunchConfigName);
                }
                sidekickInstanceIds.add(sidekickUnitInstance.getInstance().getId()
                        .intValue());
            }
        }

        return sidekickInstanceIds;
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
            DeploymentUnitInstance unitInstance = new DeploymentUnitInstanceImpl(context, service,
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

    protected List<DeploymentUnitInstance> getInstancesWithMistmatchedIndexes() {
        List<DeploymentUnitInstance> toReturn = new ArrayList<>();
        for (DeploymentUnitInstance instance : this.getDeploymentUnitInstances()) {
            if (instance.getInstance() == null) {
                continue;
            }

            String serviceIndex = DataAccessor.fieldString(instance.getInstance(),
                    InstanceConstants.FIELD_SERVICE_INSTANCE_SERVICE_INDEX);
            if (serviceIndex == null) {
                serviceIndex = ServiceConstants.getServiceIndexFromInstanceName(instance.getInstance().getName());
            }

            if (!serviceIndex.equalsIgnoreCase(unit.getServiceIndex())) {
                toReturn.add(instance);
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

    protected void sortSidekicks(List<String> sorted, String lc) {
        List<String> sidekicks = context.svcDataMgr.getLaunchConfigSidekickReferences(service, lc);
        for (String sidekick : sidekicks) {
            sortSidekicks(sorted, sidekick);
        }
        if (!sorted.contains(lc)) {
            sorted.add(lc);
        }
    }

    @Override
    public void deploy() {
        cleanupInstancesWithMistmatchedIndexes();
        cleanupUnhealthy();
        cleanupDependencies();
        create();
        start();
        waitForStart();
        updateHealthy();
    }

    public void updateHealthy() {
        if (this.isUnhealthy()) {
            return;
        }
        if (HealthcheckConstants.isHealthy(this.unit.getHealthState())) {
            context.objectProcessManager.scheduleProcessInstanceAsync(ServiceConstants.PROCESS_DU_UPDATE_HEALTHY,
                    this.unit, null);
        }
    }

    public DeploymentUnit getUnit() {
        return unit;
    }

    @Override
    public boolean isUnhealthy() {
        for (DeploymentUnitInstance instance : this.getDeploymentUnitInstances()) {
            if (instance.isUnhealthy()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void stop() {
        /*
         * stops all instances. This should be non-blocking (don't wait)
         */
        for (DeploymentUnitInstance instance : getDeploymentUnitInstances()) {
            instance.stop();
        }
    }

    public void start() {
        for (DeploymentUnitInstance instance : getSortedDeploymentUnitInstances()) {
            instance.start();
        }
    }

    protected void create() {
        createImpl();
        for (DeploymentUnitInstance instance : launchConfigToInstance.values()) {
            instance.scheduleCreate();
        }
    }

    protected void waitForStart() {
        // sort based on dependencies
        List<DeploymentUnitInstance> sortedInstances = getSortedDeploymentUnitInstances();
        for (DeploymentUnitInstance instance : sortedInstances) {
            instance.waitForStart();
        }
    }

    public List<DeploymentUnitInstance> getSortedDeploymentUnitInstances() {
        List<String> sortedLCs = new ArrayList<>();
        for (String lc : launchConfigToInstance.keySet()) {
            sortSidekicks(sortedLCs, lc);
        }

        List<DeploymentUnitInstance> sortedInstances = new ArrayList<>();
        for (String lc : sortedLCs) {
            sortedInstances.add(launchConfigToInstance.get(lc));
        }
        Collections.reverse(sortedInstances);
        return sortedInstances;
    }

    public List<DeploymentUnitInstance> getDeploymentUnitInstances() {
        List<DeploymentUnitInstance> instances = new ArrayList<>();
        instances.addAll(launchConfigToInstance.values());
        return instances;
    }

    protected void addDeploymentInstance(String launchConfig, DeploymentUnitInstance instance) {
        this.launchConfigToInstance.put(launchConfig, instance);
    }

    protected void removeDeploymentUnitInstance(DeploymentUnitInstance instance, String reason, String level) {
        instance.remove(reason, level);
        launchConfigToInstance.remove(instance.getLaunchConfigName());
    }

    @Override
    public boolean isHealthCheckInitializing() {
        for (DeploymentUnitInstance instance : getDeploymentUnitInstances()) {
            if (instance.isHealthCheckInitializing()) {
                return true;
            }
        }
        return false;
    }

    protected boolean isStarted() {
        for (DeploymentUnitInstance instance : getDeploymentUnitInstances()) {
            if (!instance.isStarted()) {
                return false;
            }
        }
        return true;
    }

    protected boolean isComplete() {
        return launchConfigToInstance.keySet().containsAll(launchConfigNames);
    }

    @Override
    public String getStatus() {
        return String.format("Healthy: %s, Complete: %s, Started: %s",
                !isUnhealthy(),
                isComplete(),
                isStarted());
    }

    protected void cleanupInstancesWithMistmatchedIndexes() {
        List<DeploymentUnitInstance> toCleanup = getInstancesWithMistmatchedIndexes();
        for (DeploymentUnitInstance i : toCleanup) {
            removeDeploymentUnitInstance(i, ServiceConstants.AUDIT_LOG_REMOVE_BAD, ActivityLog.INFO);
        }
    }

    @Override
    public boolean needToReconcile() {
        return isUnhealthy() || !isComplete() || !isStarted()
                || getInstancesWithMistmatchedIndexes().size() > 0;
    }
}
