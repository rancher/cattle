package io.cattle.platform.servicediscovery.upgrade.impl;

import static io.cattle.platform.core.model.tables.GenericObjectTable.*;
import static io.cattle.platform.core.model.tables.ServiceExposeMapTable.*;
import io.cattle.platform.activity.ActivityLog;
import io.cattle.platform.activity.ActivityService;
import io.cattle.platform.allocator.service.AllocationHelper;
import io.cattle.platform.core.addon.InServiceUpgradeStrategy;
import io.cattle.platform.core.addon.RollingRestartStrategy;
import io.cattle.platform.core.addon.ServiceRestart;
import io.cattle.platform.core.addon.ServiceUpgradeStrategy;
import io.cattle.platform.core.addon.ToServiceUpgradeStrategy;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.GenericObjectConstants;
import io.cattle.platform.core.constants.HealthcheckConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.dao.ServiceDao;
import io.cattle.platform.core.model.DeploymentUnit;
import io.cattle.platform.core.model.GenericObject;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.ServiceExposeMap;
import io.cattle.platform.engine.process.ExitReason;
import io.cattle.platform.engine.process.impl.ProcessCancelException;
import io.cattle.platform.engine.process.impl.ProcessExecutionExitException;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.lock.LockCallbackNoReturn;
import io.cattle.platform.lock.LockManager;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.resource.ResourceMonitor;
import io.cattle.platform.object.resource.ResourcePredicate;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.process.common.util.ProcessUtils;
import io.cattle.platform.servicediscovery.api.dao.ServiceExposeMapDao;
import io.cattle.platform.servicediscovery.api.util.ServiceDiscoveryUtil;
import io.cattle.platform.servicediscovery.deployment.DeploymentManager;
import io.cattle.platform.servicediscovery.deployment.DeploymentUnitManager;
import io.cattle.platform.servicediscovery.deployment.impl.lock.ServiceLock;
import io.cattle.platform.servicediscovery.service.ServiceDiscoveryService;
import io.cattle.platform.servicediscovery.upgrade.UpgradeManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

public class UpgradeManagerImpl implements UpgradeManager {

    private enum Type {
        ToUpgrade,
        ToCleanup,
        UpgradedUnmanaged,
    }

    @Inject
    ServiceExposeMapDao exposeMapDao;
    @Inject
    ObjectManager objectManager;
    @Inject
    DeploymentManager deploymentMgr;
    @Inject
    LockManager lockManager;
    @Inject
    ObjectProcessManager objectProcessMgr;
    @Inject
    ResourceMonitor resourceMntr;
    @Inject
    ServiceDiscoveryService serviceDiscoveryService;
    @Inject
    JsonMapper jsonMapper;
    @Inject
    ActivityService activityService;
    @Inject
    ServiceDao serviceDao;
    @Inject
    DeploymentUnitManager duMgr;
    @Inject
    ResourceMonitor resourceMtr;
    @Inject
    AllocationHelper allocationHelper;

    private static final long SLEEP = 1000L;

    protected void resetUpgrade(ServiceExposeMap map, boolean upgrade) {
        if (upgrade) {
            map.setUpgrade(true);
            map.setManaged(false);
            map.setUpgradeTime(new Date());
        } else {
            map.setUpgrade(false);
            map.setManaged(true);
        }
        objectManager.persist(map);
    }

    public boolean doInServiceUpgrade(Service service, InServiceUpgradeStrategy strategy, boolean isUpgrade, String currentProcess) {
        long batchSize = strategy.getBatchSize();
        boolean startFirst = strategy.getStartFirst();
        Map<String, DeploymentUnit> allUnits = serviceDao.getDeploymentUnits(service);

        Map<String, Pair<DeploymentUnit, List<Instance>>> toUpgrade = getDeploymentUnits(
                service,
                Type.ToUpgrade, isUpgrade, strategy, allUnits);

        Map<String, Pair<DeploymentUnit, List<Instance>>> upgradedUnmanaged = getDeploymentUnits(
                service,
                Type.UpgradedUnmanaged, isUpgrade, strategy, allUnits);

        Map<String, Pair<DeploymentUnit, List<Instance>>> toCleanup = getDeploymentUnits(
                service,
                Type.ToCleanup, isUpgrade, strategy, allUnits);

        // upgrade deployment units
        upgradeDeploymentUnits(service, toUpgrade, upgradedUnmanaged,
                toCleanup,
                batchSize, startFirst, isUpgrade, currentProcess, strategy);

        // check if empty
        if (toUpgrade.isEmpty()) {
            return true;
        }
        return false;
    }

    protected void upgradeDeploymentUnits(final Service service,
            final Map<String, Pair<DeploymentUnit, List<Instance>>> toUpgrade,
            final Map<String, Pair<DeploymentUnit, List<Instance>>> upgradedUnmanaged,
            final Map<String, Pair<DeploymentUnit, List<Instance>>> toCleanup,
            final long batchSize,
            final boolean startFirst, final boolean isUpgrade, final String currentProcess,
            final InServiceUpgradeStrategy strategy) {
        // hold the lock so service.reconcile triggered by config.update
        // (in turn triggered by instance.remove) won't interfere
        lockManager.lock(new ServiceLock(service), new LockCallbackNoReturn() {
            @Override
            public void doWithLockNoResult() {
                stopInstances(service, toCleanup);
                // wait for healthy only for upgrade
                // should be skipped for rollback
                if (isUpgrade) {
                    reconcileUnits(toCleanup);
                    waitForHealthyState(service, currentProcess, strategy);
                }

                // mark instances for upgrade by moving them to toCleanup list
                markForUpgrade(batchSize, toUpgrade, upgradedUnmanaged, toCleanup);

                if (startFirst) {
                    // 1. reconcile to start new instances
                    activate(service, toCleanup);
                    if (isUpgrade) {
                        waitForHealthyState(service, currentProcess, strategy);
                    }
                    // 2. stop instances
                    stopInstances(service, toCleanup);
                } else {
                    // reverse order
                    // 1. stop instances
                    stopInstances(service, toCleanup);
                    // 2. wait for reconcile (new instances will be started along)
                    activate(service, toCleanup);
                }
            }
        });
    }

    protected void markForRollback(String deploymentUnitUUIDToRollback,
            Map<String, Pair<DeploymentUnit, List<Instance>>> upgradedUnmanaged) {
        Pair<DeploymentUnit, List<Instance>> instances = upgradedUnmanaged
                .get(deploymentUnitUUIDToRollback);
        if (instances != null) {
            for (Instance instance : instances.getRight()) {
                ServiceExposeMap map = objectManager.findAny(ServiceExposeMap.class,
                        SERVICE_EXPOSE_MAP.INSTANCE_ID, instance.getId());
                resetUpgrade(map, false);
            }
        }
    }

    protected void markForUpgrade(final long batchSize, Map<String, Pair<DeploymentUnit, List<Instance>>> toUpgrade,
            Map<String, Pair<DeploymentUnit, List<Instance>>> upgradedUnmanaged,
            Map<String, Pair<DeploymentUnit, List<Instance>>> toCleanup) {
        long i = 0;
        Iterator<Map.Entry<String, Pair<DeploymentUnit, List<Instance>>>> it = toUpgrade
                .entrySet().iterator();
        while (it.hasNext() && i < batchSize) {
            Map.Entry<String, Pair<DeploymentUnit, List<Instance>>> instances = it.next();
            String deploymentUnitUUID = instances.getKey();
            markForRollback(deploymentUnitUUID, upgradedUnmanaged);
            for (Instance instance : instances.getValue().getRight()) {
                activityService.instance(instance, "mark.upgrade", "Mark for upgrade", ActivityLog.INFO);
                ServiceExposeMap map = objectManager.findAny(ServiceExposeMap.class,
                        SERVICE_EXPOSE_MAP.INSTANCE_ID, instance.getId());
                resetUpgrade(map, true);
            }

            List<Instance> instancesToCleanup = new ArrayList<>();
                instancesToCleanup.addAll(instances.getValue().getRight());
            if (toCleanup.get(deploymentUnitUUID) != null) {
                instancesToCleanup.addAll(toCleanup.get(deploymentUnitUUID).getRight());
            }

            toCleanup.put(deploymentUnitUUID, Pair.of(instances.getValue().getLeft(), instancesToCleanup));
            it.remove();
            i++;
        }
    }

    protected Map<String, Pair<DeploymentUnit, List<Instance>>> getDeploymentUnits(Service service, Type type, boolean isUpgrade,
            InServiceUpgradeStrategy strategy, Map<String, DeploymentUnit> allUnits) {
        Map<String, Pair<String, Map<String, Object>>> preUpgradeLaunchConfigNamesToVersion = new HashMap<>();
        Map<String, Pair<String, Map<String, Object>>> postUpgradeLaunchConfigNamesToVersion = new HashMap<>();
        // getting an original config set (to cover the scenario when config could be removed along with the upgrade)
        if (isUpgrade) {
            postUpgradeLaunchConfigNamesToVersion.putAll(strategy.getNameToVersionToConfig(service.getName(), false));
            preUpgradeLaunchConfigNamesToVersion.putAll(strategy.getNameToVersionToConfig(service.getName(), true));
        } else {
            postUpgradeLaunchConfigNamesToVersion.putAll(strategy.getNameToVersionToConfig(service.getName(), true));
            preUpgradeLaunchConfigNamesToVersion.putAll(strategy.getNameToVersionToConfig(service.getName(), false));
        }
        Map<String, Pair<DeploymentUnit, List<Instance>>> deploymentUnitInstances = new HashMap<>();
        // iterate over pre-upgraded state
        // get desired version from post upgrade state
        if (type == Type.UpgradedUnmanaged) {
            for (String launchConfigName : postUpgradeLaunchConfigNamesToVersion.keySet()) {
                String toVersion = postUpgradeLaunchConfigNamesToVersion.get(launchConfigName).getLeft();
                List<? extends Instance> instances = exposeMapDao.getUpgradedUnmanagedInstances(service,
                        launchConfigName, toVersion);
                for (Instance instance : instances) {
                    addInstanceToDeploymentUnits(deploymentUnitInstances, instance, allUnits);
                }
            }
        } else {
            if (type == Type.ToCleanup) {
                // add deployment unit even if launchConfig doesn't have any instances
                for (String duUUID : allUnits.keySet()) {
                    List<Instance> emptyInstances = new ArrayList<>();
                    deploymentUnitInstances.put(duUUID,
                            Pair.of(allUnits.get(duUUID), emptyInstances));
                }
            }
            for (String launchConfigName : preUpgradeLaunchConfigNamesToVersion.keySet()) {
                String toVersion = "undefined";
                Pair<String, Map<String, Object>> post = postUpgradeLaunchConfigNamesToVersion.get(launchConfigName);
                if (post != null) {
                    toVersion = post.getLeft();
                }
                List<Instance> instances = new ArrayList<>();
                if (type == Type.ToUpgrade) {
                    instances.addAll(exposeMapDao.getInstancesToUpgrade(service, launchConfigName, toVersion));
                } else if (type == Type.ToCleanup) {
                    instances.addAll(exposeMapDao.getInstancesToCleanup(service, launchConfigName, toVersion));
                }
                for (Instance instance : instances) {
                    addInstanceToDeploymentUnits(deploymentUnitInstances, instance, allUnits);
                }
            }
        }

        return deploymentUnitInstances;
    }

    protected Map<String, Pair<DeploymentUnit, List<Instance>>> formDeploymentUnitsForRestart(Service service) {
        Map<String, DeploymentUnit> allUnits = serviceDao.getDeploymentUnits(service);
        Map<String, Pair<DeploymentUnit, List<Instance>>> deploymentUnitInstances = new HashMap<>();
        List<? extends Instance> instances = getServiceInstancesToRestart(service);
        for (Instance instance : instances) {
            addInstanceToDeploymentUnits(deploymentUnitInstances, instance, allUnits);
        }
        return deploymentUnitInstances;
    }

    protected List<? extends Instance> getServiceInstancesToRestart(Service service) {
        // get all instances of the service
        List<? extends Instance> instances = exposeMapDao.listServiceManagedInstances(service);
        List<Instance> toRestart = new ArrayList<>();
        ServiceRestart svcRestart = DataAccessor.field(service, ServiceConstants.FIELD_RESTART,
                jsonMapper, ServiceRestart.class);
        RollingRestartStrategy strategy = svcRestart.getRollingRestartStrategy();
        Map<Long, Long> instanceToStartCount = strategy.getInstanceToStartCount();
        // compare its start_count with one set on the service restart field
        for (Instance instance : instances) {
            if (instanceToStartCount.containsKey(instance.getId())) {
                Long previousStartCount = instanceToStartCount.get(instance.getId());
                if (previousStartCount == instance.getStartCount()) {
                    toRestart.add(instance);
                }
            }
        }
        return toRestart;
    }

    protected void addInstanceToDeploymentUnits(
            Map<String, Pair<DeploymentUnit, List<Instance>>> deploymentUnitInstances,
            Instance instance, Map<String, DeploymentUnit> allUnits) {
        Pair<DeploymentUnit, List<Instance>> duToInstances = deploymentUnitInstances.get(instance
                .getDeploymentUnitUuid());
        List<Instance> instances = new ArrayList<Instance>();
        if (duToInstances == null) {
            duToInstances = Pair.of(allUnits.get(instance.getDeploymentUnitUuid()), instances);
        }
        instances = duToInstances.getRight();

        instances.add(instance);
        deploymentUnitInstances.put(instance.getDeploymentUnitUuid(),
                Pair.of(allUnits.get(instance.getDeploymentUnitUuid()), instances));
    }

    @Override
    public void upgrade(Service service, io.cattle.platform.core.addon.ServiceUpgradeStrategy strategy,
            String currentProcess, boolean sleep, boolean prepullImages) {
        if (strategy instanceof ToServiceUpgradeStrategy) {
            ToServiceUpgradeStrategy toServiceStrategy = (ToServiceUpgradeStrategy) strategy;
            Service toService = objectManager.loadResource(Service.class, toServiceStrategy.getToServiceId());
            if (toService == null || toService.getRemoved() != null) {
                return;
            }
            updateLinks(service, toServiceStrategy);
        }
        while (!doUpgrade(service, strategy, currentProcess, prepullImages)) {
            if (sleep) {
                sleep(service, strategy, currentProcess);
            }
        }
    }

    @Override
    public void rollback(Service service, ServiceUpgradeStrategy strategy) {
        if (strategy instanceof ToServiceUpgradeStrategy) {
            return;
        }
        while (!doInServiceUpgrade(service, (InServiceUpgradeStrategy) strategy, false,
                ServiceConstants.STATE_ROLLINGBACK)) {
            sleep(service, strategy, ServiceConstants.STATE_ROLLINGBACK);
        }
    }

    GenericObject getPullTask(Long revisionId, long accountId) {
        List<GenericObject> tasks = objectManager.find(GenericObject.class, GENERIC_OBJECT.ACCOUNT_ID, accountId,
                GENERIC_OBJECT.REMOVED, null);
        for (GenericObject task : tasks) {
            if (revisionId.equals(DataAccessor.fieldLong(task, InstanceConstants.FIELD_REVISION_ID))) {
                return task;
            }
        }
        return null;
    }

    protected void prepullImages(Service service) {
        Long revisionId = service.getRevisionId();
        if (revisionId == null) {
            return;
        }
        List<GenericObject> waitList = new ArrayList<>();
        for (String image : ServiceDiscoveryUtil.getServiceImages(service)) {
            GenericObject pullTask = getPullTask(revisionId, service.getAccountId());
            if (pullTask != null) {
                if (pullTask.getState().equalsIgnoreCase(CommonStatesConstants.ACTIVE)) {
                    continue;
                }
            } else {
                Map<String, Object> data = new HashMap<>();
                data.put(ObjectMetaDataManager.KIND_FIELD, GenericObjectConstants.KIND_PULL_TASK);
                data.put(InstanceConstants.FIELD_REVISION_ID, revisionId);
                data.put("image", image);
                data.put(ObjectMetaDataManager.ACCOUNT_FIELD, service.getAccountId());
                data.put(InstanceConstants.FIELD_LABELS,
                        ServiceDiscoveryUtil.getMergedServiceLabels(service, allocationHelper));
                pullTask = objectManager.create(GenericObject.class, data);
            }
            if (pullTask.getState().equalsIgnoreCase(CommonStatesConstants.REQUESTED)) {
                objectProcessMgr.scheduleProcessInstanceAsync(GenericObjectConstants.PROCESS_CREATE, pullTask, null);
            }
            waitList.add(pullTask);
        }

        for (GenericObject o : waitList) {
            resourceMtr.waitForState(o, CommonStatesConstants.ACTIVE);
        }
    }

    public boolean doUpgrade(Service service, io.cattle.platform.core.addon.ServiceUpgradeStrategy strategy,
            String currentProcess, boolean prepullImages) {
        if (prepullImages) {
            prepullImages(service);
        }

        if (strategy instanceof InServiceUpgradeStrategy) {
            InServiceUpgradeStrategy inService = (InServiceUpgradeStrategy) strategy;
            return doInServiceUpgrade(service, inService, true, currentProcess);
        } else {
            ToServiceUpgradeStrategy toService = (ToServiceUpgradeStrategy) strategy;
            return doToServiceUpgrade(service, toService, currentProcess);
        }
    }

    protected void updateLinks(Service service, ToServiceUpgradeStrategy strategy) {
        if (!strategy.isUpdateLinks()) {
            return;
        }

        serviceDiscoveryService.cloneConsumingServices(service, objectManager.loadResource(Service.class,
                strategy.getToServiceId()));
    }

    protected void sleep(final Service service, ServiceUpgradeStrategy strategy, final String currentProcess) {
        final long interval = strategy.getIntervalMillis();

        activityService.run(service, "sleep", String.format("Sleeping for %d seconds", interval/1000), new Runnable() {
            @Override
            public void run() {
                for (int i = 0;; i++) {
                            final long sleepTime = Math.max(0, Math.min(SLEEP, interval - i * SLEEP));
                            if (sleepTime == 0) {
                                break;
                            } else {
                                try {
                                    Thread.sleep(sleepTime);
                                } catch (InterruptedException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                            stateCheck(service, currentProcess);
                }
            }
        });
    }

    protected Service stateCheck(Service service, String currentProcess) {
        service = objectManager.reload(service);

        List<String> states = Arrays.asList(ServiceConstants.STATE_UPGRADING,
                ServiceConstants.STATE_ROLLINGBACK, ServiceConstants.STATE_RESTARTING,
                ServiceConstants.STATE_FINISHING_UPGRADE,
                CommonStatesConstants.UPDATING_ACTIVE);
        if (!states.contains(service.getState())) {
            throw new ProcessExecutionExitException(ExitReason.STATE_CHANGED);
        }

        if (StringUtils.equals(currentProcess, ServiceConstants.STATE_RESTARTING)) {
            return service;
        }
        // rollback should cancel upgrade, and vice versa
        if (!StringUtils.equals(currentProcess, service.getState())) {
            throw new ProcessExecutionExitException(ExitReason.STATE_CHANGED);
        }

        return service;
    }

    /**
     * @param fromService
     * @param strategy
     * @return true if the upgrade is done
     */
    protected boolean doToServiceUpgrade(Service fromService, ToServiceUpgradeStrategy strategy, String currentProcess) {
        Service toService = objectManager.loadResource(Service.class, strategy.getToServiceId());
        if (toService == null || toService.getRemoved() != null) {
            return true;
        }
        deploymentMgr.activate(toService);
        if (!deploymentMgr.isHealthy(toService)) {
            return false;
        }

        deploymentMgr.activate(fromService);

        fromService = objectManager.reload(fromService);
        toService = objectManager.reload(toService);

        long batchSize = strategy.getBatchSize();
        long finalScale = strategy.getFinalScale();

        long toScale = getScale(toService);
        long totalScale = getScale(fromService) + toScale;

        if (totalScale > finalScale) {
            fromService = changeScale(fromService, 0 - Math.min(batchSize, totalScale - finalScale));
        } else if (toScale < finalScale) {
            long max = Math.min(batchSize, finalScale - toScale);
            toService = changeScale(toService, Math.min(max, finalScale + batchSize - totalScale));
        }

        if (getScale(fromService) == 0 && getScale(toService) != finalScale) {
            changeScale(toService, finalScale - getScale(toService));
        }

        return getScale(fromService) == 0 && getScale(toService) == finalScale;
    }

    protected Service changeScale(Service service, long delta) {
        if (delta == 0) {
            return service;
        }

        long newScale = Math.max(0, getScale(service) + delta);

        service = objectManager.setFields(service, ServiceConstants.FIELD_SCALE, newScale);
        deploymentMgr.activate(service);
        return objectManager.reload(service);
    }

    protected int getScale(Service service) {
        Integer i = DataAccessor.fieldInteger(service, ServiceConstants.FIELD_SCALE);
        return i == null ? 0 : i;
    }

    @Override
    public void finishUpgrade(Service service, boolean reconcile) {
        // cleanup instances set for upgrade
        cleanupUpgradedInstances(service);

        // reconcile
        if (reconcile) {
            deploymentMgr.activate(service);
        }
    }

    protected void waitForHealthyState(final Service service, final String currentProcess,
            final InServiceUpgradeStrategy strategy) {
        activityService.run(service, "wait", "Waiting for all instances to be healthy", new Runnable() {
            @Override
            public void run() {
                final List<String> healthyStates = Arrays.asList(HealthcheckConstants.HEALTH_STATE_HEALTHY,
                        HealthcheckConstants.HEALTH_STATE_UPDATING_HEALTHY);
                List<? extends Instance> instancesToCheck = getInstancesToCheckForHealth(service, strategy);
                for (final Instance instance : instancesToCheck) {
                    if (instance.getState().equalsIgnoreCase(InstanceConstants.STATE_RUNNING)) {
                        resourceMntr.waitFor(instance,
                                new ResourcePredicate<Instance>() {
                                    @Override
                                    public boolean evaluate(Instance obj) {
                                        boolean healthy = instance.getHealthState() == null
                                                || healthyStates.contains(obj.getHealthState());
                                        if (!healthy) {
                                            stateCheck(service, currentProcess);
                                        }
                                        return healthy;
                                    }

                                    @Override
                                    public String getMessage() {
                                        return "healthy";
                                    }
                                });
                    }
                }
            }
        });
    }

    private List<? extends Instance> getInstancesToCheckForHealth(Service service,
            InServiceUpgradeStrategy strategy) {
        if (strategy == null) {
            return exposeMapDao.listServiceManagedInstances(service);
        }

        Map<String, String> lcToCurrentV = getLaunchConfigToCurrentVersion(service, strategy);
        List<Instance> filtered = new ArrayList<>();
        // only check upgraded instances for health
        for (String lc : lcToCurrentV.keySet()) {
            List<? extends Instance> instances = exposeMapDao.listServiceManagedInstances(service, lc);
            for (Instance instance : instances) {
                if (instance.getVersion() != null &&
                        instance.getVersion().equalsIgnoreCase(lcToCurrentV.get(lc))) {
                    filtered.add(instance);
                }
            }
        }
        return filtered;
    }

    private Map<String, String> getLaunchConfigToCurrentVersion(Service service, InServiceUpgradeStrategy strategy) {
        Map<String, String> lcToV = new HashMap<>();
        Map<String, Pair<String, Map<String, Object>>> vToC = strategy.getNameToVersionToConfig(service.getName(),
                false);
        for (String lc : vToC.keySet()) {
            lcToV.put(lc, vToC.get(lc).getLeft());
        }
        return lcToV;
    }

    public void cleanupUpgradedInstances(Service service) {
        List<? extends Instance> instances = exposeMapDao.getInstancesSetForUpgrade(service.getId());
        for (Instance instance : instances) {
            try {
                objectProcessMgr.scheduleProcessInstanceAsync(InstanceConstants.PROCESS_REMOVE,
                        instance, null);
            } catch (ProcessCancelException ex) {
                // in case instance was manually restarted
                objectProcessMgr.scheduleProcessInstanceAsync(InstanceConstants.PROCESS_STOP,
                        instance, ProcessUtils.chainInData(new HashMap<String, Object>(),
                                InstanceConstants.PROCESS_STOP, InstanceConstants.PROCESS_REMOVE));
            }
        }
    }

    @Override
    public void restart(Service service, RollingRestartStrategy strategy) {
        Map<String, Pair<DeploymentUnit, List<Instance>>> toRestart = formDeploymentUnitsForRestart(service);
        while (!doRestart(service, strategy, toRestart)) {
            sleep(service, strategy, ServiceConstants.STATE_RESTARTING);
        }
    }

    public boolean doRestart(Service service, RollingRestartStrategy strategy,
            Map<String, Pair<DeploymentUnit, List<Instance>>> toRestart) {
        long batchSize = strategy.getBatchSize();
        final Map<String, Pair<DeploymentUnit, List<Instance>>> restartBatch = new HashMap<>();
        long i = 0;
        Iterator<Map.Entry<String, Pair<DeploymentUnit, List<Instance>>>> it = toRestart.entrySet()
                .iterator();
        while (it.hasNext() && i < batchSize) {
            Map.Entry<String, Pair<DeploymentUnit, List<Instance>>> instances = it.next();
            String deploymentUnitUUID = instances.getKey();
            restartBatch.put(deploymentUnitUUID, instances.getValue());
            it.remove();
            i++;
        }

        restartDeploymentUnits(service, restartBatch);

        if (toRestart.isEmpty()) {
            return true;
        }
        return false;
    }

    protected void restartDeploymentUnits(final Service service,
            final Map<String, Pair<DeploymentUnit, List<Instance>>> deploymentUnitsToStop) {

        // hold the lock so service.reconcile triggered by config.update
        // (in turn triggered by instance.remove) won't interfere

        lockManager.lock(new ServiceLock(service), new LockCallbackNoReturn() {
            @Override
            public void doWithLockNoResult() {
                // 1. Wait for the service instances to become healthy
                waitForHealthyState(service, ServiceConstants.STATE_RESTARTING, null);
                // 2. stop instances
                stopInstances(service, deploymentUnitsToStop);
                // 3. wait for reconcile (instances will be restarted along)
                activate(service, deploymentUnitsToStop);
            }
        });
    }

    protected void activate(final Service service, final Map<String, Pair<DeploymentUnit, List<Instance>>> unitsToReconcile) {
        activityService.run(service, "starting", "Starting new instances", new Runnable() {
            @Override
            public void run() {
                reconcileUnits(unitsToReconcile);
            }
        });

    }

    public void reconcileUnits(final Map<String, Pair<DeploymentUnit, List<Instance>>> unitsToReconcile) {
        for (Pair<DeploymentUnit, List<Instance>> toReconcile : unitsToReconcile.values()) {
            duMgr.activate(toReconcile.getLeft());
        }
    }

    protected void stopInstances(Service service,
            final Map<String, Pair<DeploymentUnit, List<Instance>>> deploymentUnitInstancesToStop) {
        activityService.run(service, "stopping", "Stopping instances", new Runnable() {
            @Override
            public void run() {
                List<Instance> toStop = new ArrayList<>();
                List<Instance> toWait = new ArrayList<>();
                for (String key : deploymentUnitInstancesToStop.keySet()) {
                    toStop.addAll(deploymentUnitInstancesToStop.get(key).getRight());
                }
                for (Instance instance : toStop) {
                    HashMap<String, Object> data = new HashMap<String, Object>();
                    data.put(ServiceConstants.PROCESS_DATA_SERVICE_RECONCILE, true);
                    instance = resourceMntr.waitForNotTransitioning(instance);

                    if (CommonStatesConstants.REMOVED.equals(instance.getState())) {
                        continue;
                    } else if (InstanceConstants.STATE_ERROR.equals(instance.getState())) {
                        objectProcessMgr.scheduleProcessInstanceAsync(InstanceConstants.PROCESS_REMOVE,
                                instance, data);
                    } else if (!instance.getState().equalsIgnoreCase(InstanceConstants.STATE_STOPPED)) {
                        objectProcessMgr.scheduleProcessInstanceAsync(InstanceConstants.PROCESS_STOP,
                                instance, data);
                        toWait.add(instance);
                    }
                }
                for (Instance instance : toWait) {
                    resourceMntr.waitForState(instance, InstanceConstants.STATE_STOPPED);
                }
            }
        });
    }
}
