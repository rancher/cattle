package io.cattle.platform.servicediscovery.upgrade.impl;

import static io.cattle.platform.core.model.tables.ServiceExposeMapTable.*;
import io.cattle.platform.activity.ActivityLog;
import io.cattle.platform.activity.ActivityService;
import io.cattle.platform.core.addon.InServiceUpgradeStrategy;
import io.cattle.platform.core.addon.RollingRestartStrategy;
import io.cattle.platform.core.addon.ServiceRestart;
import io.cattle.platform.core.addon.ServiceUpgradeStrategy;
import io.cattle.platform.core.addon.ToServiceUpgradeStrategy;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.HealthcheckConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.ServiceConstants;
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
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.resource.ResourceMonitor;
import io.cattle.platform.object.resource.ResourcePredicate;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.process.common.util.ProcessUtils;
import io.cattle.platform.servicediscovery.api.dao.ServiceExposeMapDao;
import io.cattle.platform.servicediscovery.deployment.DeploymentManager;
import io.cattle.platform.servicediscovery.deployment.impl.lock.ServiceLock;
import io.cattle.platform.servicediscovery.service.ServiceDiscoveryService;
import io.cattle.platform.servicediscovery.upgrade.UpgradeManager;

import java.util.ArrayList;
import java.util.Arrays;
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

    private static final long SLEEP = 1000L;

    protected void setUpgrade(ServiceExposeMap map, boolean upgrade) {
        if (upgrade) {
            map.setUpgrade(true);
            map.setManaged(false);
        } else {
            map.setUpgrade(false);
            map.setManaged(true);
        }
        objectManager.persist(map);
    }

    public boolean doInServiceUpgrade(Service service, InServiceUpgradeStrategy strategy, boolean isUpgrade, String currentProcess) {
        long batchSize = strategy.getBatchSize();
        boolean startFirst = strategy.getStartFirst();

        Map<String, List<Instance>> deploymentUnitInstancesToUpgrade = formDeploymentUnitsForUpgrade(service,
                Type.ToUpgrade, isUpgrade, strategy);

        Map<String, List<Instance>> deploymentUnitInstancesUpgradedUnmanaged = formDeploymentUnitsForUpgrade(
                service,
                Type.UpgradedUnmanaged, isUpgrade, strategy);

        Map<String, List<Instance>> deploymentUnitInstancesToCleanup = formDeploymentUnitsForUpgrade(service,
                Type.ToCleanup, isUpgrade, strategy);

        // upgrade deployment units
        upgradeDeploymentUnits(service, deploymentUnitInstancesToUpgrade, deploymentUnitInstancesUpgradedUnmanaged,
                deploymentUnitInstancesToCleanup,
                batchSize, startFirst, preseveDeploymentUnit(service, strategy), isUpgrade, currentProcess, strategy);

        // check if empty
        if (deploymentUnitInstancesToUpgrade.isEmpty()) {
            deploymentMgr.activate(service);
            return true;
        }
        return false;
    }

    protected boolean preseveDeploymentUnit(Service service, InServiceUpgradeStrategy strategy) {
        boolean isServiceIndexDUStrategy = StringUtils.equalsIgnoreCase(
                ServiceConstants.SERVICE_INDEX_DU_STRATEGY,
                DataAccessor.fieldString(service, ServiceConstants.FIELD_SERVICE_INDEX_STRATEGY));
        return isServiceIndexDUStrategy || !strategy.isFullUpgrade();
    }

    protected void upgradeDeploymentUnits(final Service service,
            final Map<String, List<Instance>> deploymentUnitInstancesToUpgrade,
            final Map<String, List<Instance>> deploymentUnitInstancesUpgradedUnmanaged,
            final Map<String, List<Instance>> deploymentUnitInstancesToCleanup,
            final long batchSize,
            final boolean startFirst, final boolean preseveDeploymentUnit, final boolean isUpgrade,
            final String currentProcess, final InServiceUpgradeStrategy strategy) {
        // hold the lock so service.reconcile triggered by config.update
        // (in turn triggered by instance.remove) won't interfere
        lockManager.lock(new ServiceLock(service), new LockCallbackNoReturn() {
            @Override
            public void doWithLockNoResult() {
                // wait for healthy only for upgrade
                // should be skipped for rollback
                if (isUpgrade) {
                    deploymentMgr.activate(service);
                    waitForHealthyState(service, currentProcess, strategy);
                }
                // mark for upgrade
                markForUpgrade(batchSize);

                if (startFirst) {
                    // 1. reconcile to start new instances
                    activate(service);
                    if (isUpgrade) {
                        waitForHealthyState(service, currentProcess, strategy);
                    }
                    // 2. stop instances
                    stopInstances(service, deploymentUnitInstancesToCleanup);
                } else {
                    // reverse order
                    // 1. stop instances
                    stopInstances(service, deploymentUnitInstancesToCleanup);
                    // 2. wait for reconcile (new instances will be started along)
                    activate(service);
                }
            }

            protected void markForUpgrade(final long batchSize) {
                markForCleanup(batchSize, preseveDeploymentUnit);
            }

            protected void markForCleanup(final long batchSize,
                    boolean preseveDeploymentUnit) {
                long i = 0;
                Iterator<Map.Entry<String, List<Instance>>> it = deploymentUnitInstancesToUpgrade.entrySet()
                        .iterator();
                while (it.hasNext() && i < batchSize) {
                    Map.Entry<String, List<Instance>> instances = it.next();
                    String deploymentUnitUUID = instances.getKey();
                    markForRollback(deploymentUnitUUID);
                    for (Instance instance : instances.getValue()) {
                        activityService.instance(instance, "mark.upgrade", "Mark for upgrade", ActivityLog.INFO);
                        ServiceExposeMap map = objectManager.findAny(ServiceExposeMap.class,
                                SERVICE_EXPOSE_MAP.INSTANCE_ID, instance.getId());
                        setUpgrade(map, true);
                    }
                    deploymentUnitInstancesToCleanup.put(deploymentUnitUUID, instances.getValue());
                    it.remove();
                    i++;
                }
            }

            protected void markForRollback(String deploymentUnitUUIDToRollback) {
                List<Instance> instances = new ArrayList<>();
                if (preseveDeploymentUnit) {
                    instances = deploymentUnitInstancesUpgradedUnmanaged.get(deploymentUnitUUIDToRollback);
                } else {
                    // when preserveDeploymentunit == false, we don't care what deployment unit needs to be rolled back
                    String toExtract = null;
                    for (String key : deploymentUnitInstancesUpgradedUnmanaged.keySet()) {
                        if (toExtract != null) {
                            break;
                        }
                        toExtract = key;
                    }
                    instances = deploymentUnitInstancesUpgradedUnmanaged.get(toExtract);
                    deploymentUnitInstancesUpgradedUnmanaged.remove(toExtract);
                }
                if (instances != null) {
                    for (Instance instance : instances) {
                        ServiceExposeMap map = objectManager.findAny(ServiceExposeMap.class,
                                SERVICE_EXPOSE_MAP.INSTANCE_ID, instance.getId());
                        setUpgrade(map, false);
                    }
                }
            }
        });
    }

    protected Map<String, List<Instance>> formDeploymentUnitsForUpgrade(Service service, Type type, boolean isUpgrade,
            InServiceUpgradeStrategy strategy) {
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
        Map<String, List<Instance>> deploymentUnitInstances = new HashMap<>();
        // iterate over pre-upgraded state
        // get desired version from post upgrade state
        if (type == Type.UpgradedUnmanaged) {
            for (String launchConfigName : postUpgradeLaunchConfigNamesToVersion.keySet()) {
                List<Instance> instances = new ArrayList<>();
                Pair<String, Map<String, Object>> post = postUpgradeLaunchConfigNamesToVersion.get(launchConfigName);
                String toVersion = post.getLeft();
                instances.addAll(exposeMapDao.getUpgradedInstances(service,
                        launchConfigName, toVersion, false));
                for (Instance instance : instances) {
                    addInstanceToDeploymentUnits(deploymentUnitInstances, instance);
                }
            }
        } else {
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
                    addInstanceToDeploymentUnits(deploymentUnitInstances, instance);
                }
            }
        }

        return deploymentUnitInstances;
    }

    protected Map<String, List<Instance>> formDeploymentUnitsForRestart(Service service) {
        Map<String, List<Instance>> deploymentUnitInstances = new HashMap<>();
        List<? extends Instance> instances = getServiceInstancesToRestart(service);
        for (Instance instance : instances) {
            addInstanceToDeploymentUnits(deploymentUnitInstances, instance);
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

    protected void addInstanceToDeploymentUnits(Map<String, List<Instance>> deploymentUnitInstancesToUpgrade,
            Instance instance) {
        List<Instance> toRemove = deploymentUnitInstancesToUpgrade.get(instance.getDeploymentUnitUuid());
        if (toRemove == null) {
            toRemove = new ArrayList<Instance>();
        }
        toRemove.add(instance);
        deploymentUnitInstancesToUpgrade.put(instance.getDeploymentUnitUuid(), toRemove);
    }

    @Override
    public void upgrade(Service service, io.cattle.platform.core.addon.ServiceUpgradeStrategy strategy, String currentProcess) {
        if (strategy instanceof ToServiceUpgradeStrategy) {
            ToServiceUpgradeStrategy toServiceStrategy = (ToServiceUpgradeStrategy) strategy;
            Service toService = objectManager.loadResource(Service.class, toServiceStrategy.getToServiceId());
            if (toService == null || toService.getRemoved() != null) {
                return;
            }
            updateLinks(service, toServiceStrategy);
        }
        while (!doUpgrade(service, strategy, currentProcess)) {
            sleep(service, strategy, currentProcess);
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

    public boolean doUpgrade(Service service, io.cattle.platform.core.addon.ServiceUpgradeStrategy strategy,
            String currentProcess) {
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
                ServiceConstants.STATE_FINISHING_UPGRADE);
        if (!states.contains(service.getState())) {
            throw new ProcessExecutionExitException(ExitReason.STATE_CHANGED);
        }

        if (StringUtils.equals(currentProcess, ServiceConstants.STATE_RESTARTING)) {
            return service;
        }
        // rollback should cancel upgarde, and vice versa
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
        List<? extends ServiceExposeMap> maps = exposeMapDao.getInstancesSetForUpgrade(service.getId());
        List<Instance> waitList = new ArrayList<>();
        for (ServiceExposeMap map : maps) {
            Instance instance = objectManager.loadResource(Instance.class, map.getInstanceId());
            if (instance == null || instance.getState().equals(CommonStatesConstants.REMOVED) || instance.getState().equals(
                            CommonStatesConstants.REMOVING)) {
                continue;
            }
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

        for (Instance instance : waitList) {
            resourceMntr.waitForState(instance, CommonStatesConstants.REMOVED);
        }
    }

    @Override
    public void restart(Service service, RollingRestartStrategy strategy) {
        Map<String, List<Instance>> toRestart = formDeploymentUnitsForRestart(service);
        while (!doRestart(service, strategy, toRestart)) {
            sleep(service, strategy, ServiceConstants.STATE_RESTARTING);
        }
    }

    public boolean doRestart(Service service, RollingRestartStrategy strategy,
            Map<String, List<Instance>> toRestart) {
        long batchSize = strategy.getBatchSize();
        final Map<String, List<Instance>> restartBatch = new HashMap<>();
        long i = 0;
        Iterator<Map.Entry<String, List<Instance>>> it = toRestart.entrySet()
                .iterator();
        while (it.hasNext() && i < batchSize) {
            Map.Entry<String, List<Instance>> instances = it.next();
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
            final Map<String, List<Instance>> deploymentUnitsToStop) {

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
                activate(service);
            }
        });
    }

    protected void activate(final Service service) {
        activityService.run(service, "starting", "Starting new instances", new Runnable() {
            @Override
            public void run() {
                deploymentMgr.activate(service);
            }
        });

    }
    protected void stopInstances(Service service, final Map<String, List<Instance>> deploymentUnitInstancesToStop) {
        activityService.run(service, "stopping", "Stopping instances", new Runnable() {
            @Override
            public void run() {
                List<Instance> toStop = new ArrayList<>();
                List<Instance> toWait = new ArrayList<>();
                for (String key : deploymentUnitInstancesToStop.keySet()) {
                    toStop.addAll(deploymentUnitInstancesToStop.get(key));
                }
                for (Instance instance : toStop) {
                    instance = resourceMntr.waitForNotTransitioning(instance);
                    if (InstanceConstants.STATE_ERROR.equals(instance.getState())) {
                        objectProcessMgr.scheduleProcessInstanceAsync(InstanceConstants.PROCESS_REMOVE,
                                instance, null);
                    } else if (!instance.getState().equalsIgnoreCase(InstanceConstants.STATE_STOPPED)) {
                        objectProcessMgr.scheduleProcessInstanceAsync(InstanceConstants.PROCESS_STOP,
                                instance, null);
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
