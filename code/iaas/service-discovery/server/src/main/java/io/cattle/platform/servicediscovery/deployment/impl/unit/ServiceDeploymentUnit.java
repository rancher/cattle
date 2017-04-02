package io.cattle.platform.servicediscovery.deployment.impl.unit;

import static io.cattle.platform.core.model.tables.ServiceIndexTable.*;
import static io.cattle.platform.core.model.tables.ServiceTable.*;
import static io.cattle.platform.core.model.tables.StackTable.*;
import static io.cattle.platform.core.model.tables.VolumeTable.*;
import io.cattle.platform.activity.ActivityLog;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.ExternalEventConstants;
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
import io.cattle.platform.servicediscovery.deployment.impl.instance.ServiceDeploymentUnitInstance;
import io.cattle.platform.servicediscovery.deployment.impl.manager.DeploymentUnitManagerImpl.DeploymentUnitManagerContext;
import io.cattle.platform.util.exception.DeploymentUnitReconcileException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;

public class ServiceDeploymentUnit extends AbstractDeploymentUnit {
    Service service;
    Stack stack;
    Map<String, ServiceIndex> launchConfigToServiceIndexes = new HashMap<>();

    public ServiceDeploymentUnit(DeploymentUnit unit, DeploymentUnitManagerContext context) {
        super(unit, context);
        this.service = context.objectManager.findOne(Service.class, SERVICE.ID, unit.getServiceId());
        this.stack = context.objectManager.findOne(Stack.class, STACK.ID, service.getStackId());
        this.launchConfigNames = context.svcDataMgr.getServiceLaunchConfigNames(service);
        collectDeploymentUnitInstances();
        generateSidekickReferences();
    }
    
    @SuppressWarnings("unchecked")
    protected void createServiceIndex(String launchConfigName) {
        // create index
        ServiceIndex serviceIndexObj = context.serviceDao.createServiceIndex(service, launchConfigName,
                unit.getServiceIndex());

        // allocate ip address if not set
        if (DataAccessor.fieldBool(service, ServiceConstants.FIELD_RETAIN_IP)) {
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

    @Override
    protected void collectDeploymentUnitInstances() {
        collectDeploymentUnitInstances(true);
        collectDeploymentUnitInstances(false);
    }

    @SuppressWarnings("unchecked")
    public void collectDeploymentUnitInstances(boolean currentRevision) {
        List<Pair<Instance, ServiceExposeMap>> serviceInstances = context.exposeMapDao
                .listDeploymentUnitInstances(service, unit, currentRevision);
        for (Pair<Instance, ServiceExposeMap> serviceInstance : serviceInstances) {
            Instance instance = serviceInstance.getLeft();
            ServiceExposeMap exposeMap = serviceInstance.getRight();
            Map<String, String> instanceLabels = DataAccessor.fields(instance)
                    .withKey(InstanceConstants.FIELD_LABELS).withDefault(Collections.EMPTY_MAP).as(Map.class);
            String launchConfigName = instanceLabels
                    .get(ServiceConstants.LABEL_SERVICE_LAUNCH_CONFIG);
            DeploymentUnitInstance unitInstance = new ServiceDeploymentUnitInstance(context, service,
                    stack, instance.getName(), instance, exposeMap, launchConfigName);
            if (currentRevision) {
                addDeploymentInstance(launchConfigName, unitInstance);
            } else {
                oldRevisions.add(unitInstance);
            }
        }
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
    public void remove(String reason, String level) {
        removeAllDeploymentUnitInstances(reason, level);
        cleanupVolumes(true);
        cleanupServiceIndexes();
    }

    @Override
    protected void cleanupBadAndUnhealthy() {
        if (!isBad()) {
            return;
        }
        Date originalCleanupTime = context.objectManager.reload(unit).getCleanupTime();
        for (DeploymentUnitInstance instance : getDeploymentUnitInstances()) {
            if (instance.isUnhealthy()) {
                instance.remove(ServiceConstants.AUDIT_LOG_REMOVE_UNHEATLHY, ActivityLog.ERROR);
            } else {
                instance.remove(ServiceConstants.AUDIT_LOG_REMOVE_BAD, ActivityLog.ERROR);
            }

        }
        cleanupVolumes(false);
        // handle the case when cleanup was reset in one of the handlers
        Date currentCleanupTime = context.objectManager.reload(unit).getCleanupTime();
        boolean oneIsNull = (currentCleanupTime == null) != (originalCleanupTime == null);
        boolean bothNotNull = currentCleanupTime != null && originalCleanupTime != null;
        if (oneIsNull || (bothNotNull && currentCleanupTime.after(originalCleanupTime))) {
            throw new DeploymentUnitReconcileException("Need to restart deployment unit reconcile");
        }
        context.serviceDao.setForCleanup(unit, false);
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

    protected List<DeploymentUnitInstance> getInstancesWithMismatchedIndexes() {
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

    protected void addMissingInstance(String launchConfigName) {
        if (!launchConfigToInstance.containsKey(launchConfigName)) {
            String instanceName = getInstanceName(launchConfigName);
            DeploymentUnitInstance deploymentUnitInstance = new ServiceDeploymentUnitInstance(context, service,
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

    protected void createImpl() {
        for (String launchConfigName : launchConfigNames) {
            createServiceIndex(launchConfigName);
        }
        for (String launchConfigName : launchConfigNames) {
            getOrCreateInstance(launchConfigName);
        }
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
    protected List<String> getSidekickRefs(String launchConfigName) {
        return context.svcDataMgr.getLaunchConfigSidekickReferences(service, launchConfigName);
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

    protected Map<String, List<String>> getUsedBySidekicks() {
        return context.svcDataMgr.getUsedBySidekicks(service);
    }

    @Override
    protected void generateSidekickReferences() {
        Map<String, List<String>> usedBy = getUsedBySidekicks();
        sidekickUsedByMap.putAll(usedBy);
        dependees.addAll(usedBy.keySet());
    }

    @Override
    protected boolean startFirstOnUpgrade() {
        return DataAccessor.fieldBool(service, ServiceConstants.FIELD_START_FIRST_ON_UPGRADE);
    }
}
