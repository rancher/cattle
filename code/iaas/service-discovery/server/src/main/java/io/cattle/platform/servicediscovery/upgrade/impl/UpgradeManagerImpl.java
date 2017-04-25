package io.cattle.platform.servicediscovery.upgrade.impl;

import io.cattle.platform.activity.ActivityService;
import io.cattle.platform.core.addon.RollingRestartStrategy;
import io.cattle.platform.core.addon.ServiceRestart;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.dao.InstanceDao;
import io.cattle.platform.core.dao.ServiceDao;
import io.cattle.platform.core.dao.ServiceExposeMapDao;
import io.cattle.platform.core.model.DeploymentUnit;
import io.cattle.platform.core.model.GenericObject;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.util.ServiceUtil;
import io.cattle.platform.engine.process.ExitReason;
import io.cattle.platform.engine.process.impl.ProcessCancelException;
import io.cattle.platform.engine.process.impl.ProcessExecutionExitException;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.lock.LockCallbackNoReturn;
import io.cattle.platform.lock.LockManager;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.object.resource.ResourceMonitor;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.process.common.util.ProcessUtils;
import io.cattle.platform.servicediscovery.deployment.DeploymentUnitManager;
import io.cattle.platform.servicediscovery.deployment.impl.lock.ServiceLock;
import io.cattle.platform.servicediscovery.service.DeploymentManager;
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
    InstanceDao instanceDao;
    @Inject
    ServiceDiscoveryService sdService;

    private static final long SLEEP = 1000L;

    public boolean doInServiceUpgrade(Service service, long batchSize, boolean isUpgrade) {
        List<DeploymentUnit> upgraded = serviceDao.getDeploymentUnitsForRevision(service, true);
        List<DeploymentUnit> toUpgrade = serviceDao.getDeploymentUnitsForRevision(service, false);

        // upgrade deployment units
        upgradeDeploymentUnits(service, toUpgrade, upgraded,
                batchSize, isUpgrade);

        // check if empty
        if (toUpgrade.isEmpty()) {
            return true;
        }
        return false;
    }

    protected void upgradeDeploymentUnits(final Service service,
            final List<DeploymentUnit> toUpgrade,
            final List<DeploymentUnit> upgraded,
            final long batchSize,
            final boolean isUpgrade) {
        // hold the lock so service.reconcile triggered by config.update
        // (in turn triggered by instance.remove) won't interfere
        lockManager.lock(new ServiceLock(service), new LockCallbackNoReturn() {
            @Override
            public void doWithLockNoResult() {
                // wait for healthy only for upgrade
                // should be skipped for rollback
                if (isUpgrade) {
                    reconcileUnits(service, upgraded);
                }

                // mark instances for upgrade by moving them to toCleanup list
                markForUpgrade(service, batchSize, toUpgrade, upgraded);
                reconcileUnits(service, upgraded);
            }
        });
    }

    protected void markForUpgrade(Service service, long batchSize, List<DeploymentUnit> toUpgrade,
            List<DeploymentUnit> upgraded) {
        long i = 0;
        Iterator<DeploymentUnit> it = toUpgrade.iterator();
        while (it.hasNext() && i < batchSize) {
            DeploymentUnit unit = it.next();
            objectManager.setFields(objectManager.reload(unit), InstanceConstants.FIELD_REVISION_ID,
                    service.getRevisionId());
            upgraded.add(unit);
            it.remove();
            i++;
        }
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
    public void upgrade(Service service, String currentProcess, boolean sleep,
            boolean prepullImages) {
        Pair<Long, Long> batchAndInterval = ServiceUtil.getBatchSizeAndInterval(service);
        while (!doUpgrade(service, batchAndInterval.getLeft(), prepullImages)) {
            if (sleep) {
                sleep(service, batchAndInterval.getRight(), currentProcess);
            }
        }
        sdService.resetUpgradeFlag(service);
    }

    protected void prepullServiceImages(Service service) {
        Long revisionId = service.getRevisionId();
        if (revisionId == null) {
            return;
        }
        List<String> images = ServiceUtil.getServiceImagesToPrePull(service);
        if (images.isEmpty()) {
            return;
        }

        List<GenericObject> pullTasks = instanceDao.getImagePullTasks(service.getAccountId(), images, ServiceUtil.getMergedServiceLabels(service));
        for (GenericObject pullTask : pullTasks) {
            if (pullTask.getState().equalsIgnoreCase(CommonStatesConstants.REQUESTED)) {
                objectProcessMgr.scheduleStandardProcessAsync(StandardProcess.CREATE, pullTask, null);
            }
        }

        for (GenericObject pullTask : pullTasks) {
            resourceMtr.waitForState(pullTask, CommonStatesConstants.ACTIVE);
        }
    }

    public boolean doUpgrade(Service service, long batchSize, boolean prepullImages) {
        if (prepullImages) {
            prepullServiceImages(service);
        }

        return doInServiceUpgrade(service, batchSize, true);
    }


    protected void sleep(final Service service, long interval, final String currentProcess) {
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

    @Override
    public void finishUpgrade(Service service, boolean reconcile) {
        // cleanup instances set for upgrade
        cleanupUpgradedInstances(service);

        // reconcile
        if (reconcile) {
            deploymentMgr.activate(service);
        }
        objectManager.setFields(objectManager.reload(service), ServiceConstants.FIELD_FINISH_UPGRADE, false);
    }


    public void cleanupUpgradedInstances(Service service) {
        List<? extends Instance> instances = exposeMapDao.getServiceInstancesSetForUpgrade(service.getId());
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
            sleep(service, strategy.getIntervalMillis(), ServiceConstants.STATE_RESTARTING);
        }
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
        List<DeploymentUnit> toStop = new ArrayList<>();
        for (Pair<DeploymentUnit, List<Instance>> i : deploymentUnitsToStop.values()) {
            toStop.add(i.getLeft());
        }
        lockManager.lock(new ServiceLock(service), new LockCallbackNoReturn() {
            @Override
            public void doWithLockNoResult() {
                // 2. stop instances
                stopInstances(service, deploymentUnitsToStop);
                // 3. wait for reconcile (instances will be restarted along)
                reconcileUnits(service, toStop);
            }
        });
    }

    public void reconcileUnits(final Service service, final List<DeploymentUnit> unitsToReconcile) {
        activityService.run(service, "reconciling", "Reconciling deployment units", new Runnable() {
            @Override
            public void run() {
                for (DeploymentUnit toReconcile : unitsToReconcile) {
                    duMgr.activate(toReconcile);
                }
            }
        });
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
