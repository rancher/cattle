package io.cattle.platform.servicediscovery.upgrade.impl;

import static io.cattle.platform.core.model.tables.ServiceExposeMapTable.SERVICE_EXPOSE_MAP;
import io.cattle.platform.async.utils.TimeoutException;
import io.cattle.platform.core.addon.InServiceUpgradeStrategy;
import io.cattle.platform.core.addon.RollingRestartStrategy;
import io.cattle.platform.core.addon.ServiceRestart;
import io.cattle.platform.core.addon.ServiceUpgradeStrategy;
import io.cattle.platform.core.addon.ToServiceUpgradeStrategy;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.ServiceExposeMap;
import io.cattle.platform.engine.process.ExitReason;
import io.cattle.platform.engine.process.impl.ProcessCancelException;
import io.cattle.platform.engine.process.impl.ProcessExecutionExitException;
import io.cattle.platform.lock.LockCallbackNoReturn;
import io.cattle.platform.lock.LockManager;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.resource.ResourceMonitor;
import io.cattle.platform.object.resource.ResourcePredicate;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.process.common.util.ProcessUtils;
import io.cattle.platform.servicediscovery.api.constants.ServiceDiscoveryConstants;
import io.cattle.platform.servicediscovery.api.dao.ServiceExposeMapDao;
import io.cattle.platform.servicediscovery.api.util.ServiceDiscoveryUtil;
import io.cattle.platform.servicediscovery.deployment.DeploymentManager;
import io.cattle.platform.servicediscovery.deployment.impl.ServicesSidekickLock;
import io.cattle.platform.servicediscovery.service.ServiceDiscoveryService;
import io.cattle.platform.servicediscovery.upgrade.UpgradeManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

public class UpgradeManagerImpl implements UpgradeManager {

    private enum Type {
        ToUpgrade,
        ToCleanup,
        UpgradedManaged,
        UpgradedUnmanaged,
        ToRestart
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

    public boolean doInServiceUpgrade(Service service, InServiceUpgradeStrategy strategy) {
        try {
            long batchSize = strategy.getBatchSize();
            boolean startFirst = strategy.getStartFirst();

            Map<String, List<Instance>> deploymentUnitInstancesToUpgrade = formDeploymentUnits(service, Type.ToUpgrade);

            Map<String, List<Instance>> deploymentUnitInstancesUpgradedManaged = formDeploymentUnits(service,
                    Type.UpgradedManaged);

            Map<String, List<Instance>> deploymentUnitInstancesUpgradedUnmanaged = formDeploymentUnits(service,
                    Type.UpgradedUnmanaged);

            Map<String, List<Instance>> deploymentUnitInstancesToCleanup = formDeploymentUnits(service, Type.ToCleanup);

            // upgrade deployment units
            upgradeDeploymentUnits(service, deploymentUnitInstancesToUpgrade, deploymentUnitInstancesUpgradedManaged,
                    deploymentUnitInstancesUpgradedUnmanaged,
                    deploymentUnitInstancesToCleanup, batchSize, startFirst);

            // check if empty
            if (deploymentUnitInstancesToUpgrade.isEmpty()) {
                return true;
            }
            return false;

        } catch (TimeoutException e) {
            return false;
        }
    }

    protected void upgradeDeploymentUnits(final Service service,
            final Map<String, List<Instance>> deploymentUnitInstancesToUpgrade,
            final Map<String, List<Instance>> deploymentUnitInstancesUpgradedManaged,
            final Map<String, List<Instance>> deploymentUnitInstancesUpgradedUnmanaged,
            final Map<String, List<Instance>> deploymentUnitInstancesToCleanup,
            final long batchSize, final boolean startFirst) {
        // hold the lock so service.reconcile triggered by config.update
        // (in turn triggered by instance.remove) won't interfere
        lockManager.lock(new ServicesSidekickLock(Arrays.asList(service)), new LockCallbackNoReturn() {
            @Override
            public void doWithLockNoResult() {
                // 1. mark for upgrade
                markForUpgrade(deploymentUnitInstancesToUpgrade, deploymentUnitInstancesUpgradedManaged,
                        deploymentUnitInstancesUpgradedUnmanaged,
                        deploymentUnitInstancesToCleanup, batchSize);

                if (startFirst) {
                    // 1. reconcile to start new instances
                    reconcile(service);
                    // 2. stop instances
                    stopInstances(deploymentUnitInstancesToCleanup);
                } else {
                    // reverse order
                    // 1. stop instances
                    stopInstances(deploymentUnitInstancesToCleanup);
                    // 2. wait for reconcile (new instances will be started along)
                    reconcile(service);
                }
            }

            protected void markForUpgrade(final Map<String, List<Instance>> deploymentUnitInstancesToUpgrade,
                    Map<String, List<Instance>> deploymentUnitInstancesUpgradedManaged,
                    Map<String, List<Instance>> deploymentUnitInstancesUpgradedUnmanaged,
                    Map<String, List<Instance>> deploymentUnitInstancesToCleanup, final long batchSize) {

                markForCleanup(deploymentUnitInstancesToUpgrade, deploymentUnitInstancesUpgradedManaged,
                        deploymentUnitInstancesToCleanup, batchSize);
            }

            protected void markForRollback(Map<String, List<Instance>> deploymentUnitInstancesUpgradedManaged,
                    String deploymentUnitUUIDToRollback) {
                List<Instance> instances = deploymentUnitInstancesUpgradedUnmanaged.get(deploymentUnitUUIDToRollback);
                if (instances != null) {
                    for (Instance instance : instances) {
                        ServiceExposeMap map = objectManager.findAny(ServiceExposeMap.class,
                                SERVICE_EXPOSE_MAP.INSTANCE_ID, instance.getId());
                        setUpgrade(map, false);
                    }
                    deploymentUnitInstancesUpgradedManaged.put(deploymentUnitUUIDToRollback, instances);
                }
            }

            protected void markForCleanup(final Map<String, List<Instance>> deploymentUnitInstancesToUpgrade,
                    Map<String, List<Instance>> deploymentUnitInstancesUpgradedManaged,
                    Map<String, List<Instance>> deploymentUnitInstancesToCleanup, final long batchSize) {
                long i = 0;
                Iterator<Map.Entry<String, List<Instance>>> it = deploymentUnitInstancesToUpgrade.entrySet()
                        .iterator();
                while (it.hasNext() && i < batchSize) {
                    Map.Entry<String, List<Instance>> instances = it.next();
                    String deploymentUnitUUID = instances.getKey();
                    markForRollback(deploymentUnitInstancesUpgradedManaged, deploymentUnitUUID);
                    for (Instance instance : instances.getValue()) {
                        ServiceExposeMap map = objectManager.findAny(ServiceExposeMap.class,
                                SERVICE_EXPOSE_MAP.INSTANCE_ID, instance.getId());
                        setUpgrade(map, true);
                    }
                    deploymentUnitInstancesToCleanup.put(deploymentUnitUUID, instances.getValue());
                    it.remove();
                    i++;
                }
            }
        });

    }

    protected Map<String, List<Instance>> formDeploymentUnits(Service service, Type type) {
        List<String> launchConfigNames = ServiceDiscoveryUtil.getServiceLaunchConfigNames(service);
        Map<String, List<Instance>> deploymentUnitInstances = new HashMap<>();
        for (String launchConfigName : launchConfigNames) {
            String toVersion = ServiceDiscoveryUtil.getLaunchConfigObject(service,
                    launchConfigName,
                    ServiceDiscoveryConstants.FIELD_VERSION).toString();
            List<Instance> instances = new ArrayList<>();
            if (type == Type.ToUpgrade) {
                instances.addAll(exposeMapDao.getInstancesToUpgrade(service, launchConfigName, toVersion));
            } else if (type == Type.UpgradedManaged) {
                instances.addAll(exposeMapDao.getUpgradedInstances(service,
                        launchConfigName, toVersion, true));
            } else if (type == Type.ToCleanup) {
                instances.addAll(exposeMapDao.getInstancesToCleanup(service, launchConfigName, toVersion));
            } else if (type == Type.UpgradedUnmanaged) {
                instances.addAll(exposeMapDao.getUpgradedInstances(service,
                        launchConfigName, toVersion, false));
            } else if (type == Type.ToRestart) {
                instances.addAll(getServiceInstancesToRestart(service));
            }
            for (Instance instance : instances) {
                addInstanceToDeploymentUnits(deploymentUnitInstances, instance);
            }
        }
        
        return deploymentUnitInstances;
    }

    protected List<? extends Instance> getServiceInstancesToRestart(Service service) {
        // get all instances of the service
        List<? extends Instance> instances = exposeMapDao.listServiceManagedInstances(service.getId());
        List<Instance> toRestart = new ArrayList<>();
        ServiceRestart svcRestart = DataAccessor.field(service, ServiceDiscoveryConstants.FIELD_RESTART,
                ServiceRestart.class);
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
    public void upgrade(Service service, io.cattle.platform.core.addon.ServiceUpgradeStrategy strategy) {
        /*
         * TODO: move this and all downstream methods to a UpgradeManager with pluggable
         * strategies
         */
        if (strategy instanceof ToServiceUpgradeStrategy) {
            ToServiceUpgradeStrategy toServiceStrategy = (ToServiceUpgradeStrategy) strategy;
            Service toService = objectManager.loadResource(Service.class, toServiceStrategy.getToServiceId());
            if (toService == null || toService.getRemoved() != null) {
                return;
            }
            updateLinks(service, toServiceStrategy);
        }
        while (!doUpgrade(service, strategy)) {
            sleep(service, strategy);
        }
    }


    @Override
    public void rollback(Service service, ServiceUpgradeStrategy strategy) {
        if (strategy instanceof ToServiceUpgradeStrategy) {
            return;
        }
        while (!doInServiceUpgrade(service, (InServiceUpgradeStrategy) strategy)) {
            sleep(service, strategy);
        }
    }

    public boolean doUpgrade(Service service, io.cattle.platform.core.addon.ServiceUpgradeStrategy strategy) {
        if (strategy instanceof InServiceUpgradeStrategy) {
            InServiceUpgradeStrategy inService = (InServiceUpgradeStrategy) strategy;
            return doInServiceUpgrade(service, inService);
        } else {
            ToServiceUpgradeStrategy toService = (ToServiceUpgradeStrategy) strategy;
            return doToServiceUpgrade(service, toService);
        }
    }

    protected void updateLinks(Service service, ToServiceUpgradeStrategy strategy) {
        if (!strategy.isUpdateLinks()) {
            return;
        }

        serviceDiscoveryService.cloneConsumingServices(service, objectManager.loadResource(Service.class,
                strategy.getToServiceId()));
    }

    protected Service sleep(Service service, ServiceUpgradeStrategy strategy) {
        long interval = strategy.getIntervalMillis();

        for (int i = 0;; i++) {
            long sleepTime = Math.max(0, Math.min(SLEEP, interval - i * SLEEP));
            if (sleepTime == 0) {
                break;
            } else {
                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

            service = reload(service);
        }

        return service;
    }

    protected Service reload(Service service) {
        service = objectManager.reload(service);
        
        List<String> states = Arrays.asList(ServiceDiscoveryConstants.STATE_UPGRADING,
                ServiceDiscoveryConstants.STATE_ROLLINGBACK, ServiceDiscoveryConstants.STATE_RESTARTING);
        if (!states.contains(service.getState())) {
            throw new ProcessExecutionExitException(ExitReason.STATE_CHANGED);
        }

        return service;
    }

    /**
     * @param fromService
     * @param strategy
     * @return true if the upgrade is done
     */
    protected boolean doToServiceUpgrade(Service fromService, ToServiceUpgradeStrategy strategy) {
        Service toService = objectManager.loadResource(Service.class, strategy.getToServiceId());
        if (toService == null || toService.getRemoved() != null) {
            return true;
        }

        try {
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
        } catch (TimeoutException e) {

            return false;
        }
    }

    protected Service changeScale(Service service, long delta) {
        if (delta == 0) {
            return service;
        }

        long newScale = Math.max(0, getScale(service) + delta);

        service = objectManager.setFields(service, ServiceDiscoveryConstants.FIELD_SCALE, newScale);
        deploymentMgr.activate(service);
        return objectManager.reload(service);
    }

    protected int getScale(Service service) {
        Integer i = DataAccessor.fieldInteger(service, ServiceDiscoveryConstants.FIELD_SCALE);
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

    public void cleanupUpgradedInstances(Service service) {
        List<? extends ServiceExposeMap> maps = exposeMapDao.getInstancesSetForUpgrade(service.getId());
        List<Instance> waitList = new ArrayList<>();
        for (ServiceExposeMap map : maps) {
            Instance instance = objectManager.loadResource(Instance.class, map.getInstanceId());
            if (instance == null) {
                continue;
            }
            try {
                // in case instance was manually restarted
                objectProcessMgr.scheduleProcessInstanceAsync(InstanceConstants.PROCESS_STOP,
                        instance, ProcessUtils.chainInData(new HashMap<String, Object>(),
                                InstanceConstants.PROCESS_STOP, InstanceConstants.PROCESS_REMOVE));
            } catch (ProcessCancelException ex) {
                objectProcessMgr.scheduleProcessInstanceAsync(InstanceConstants.PROCESS_REMOVE,
                        instance, null);
            }
        }

        for (Instance instance : waitList) {
            resourceMntr.waitFor(instance,
                    new ResourcePredicate<Instance>() {
                        @Override
                        public boolean evaluate(Instance obj) {
                            return CommonStatesConstants.REMOVED.equals(obj.getState());
                        }
                    });
        }
    }

    @Override
    public void restart(Service service, RollingRestartStrategy strategy) {
        Map<String, List<Instance>> toRestart = formDeploymentUnits(service, Type.ToRestart);
        while (!doRestart(service, strategy, toRestart)) {
            sleep(service, strategy);
        }
    }

    public boolean doRestart(Service service, RollingRestartStrategy strategy,
            Map<String, List<Instance>> toRestart) {
        try {
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

        } catch (TimeoutException e) {
            return false;
        }
    }

    protected void restartDeploymentUnits(final Service service,
            final Map<String, List<Instance>> deploymentUnitsToStop) {

        // hold the lock so service.reconcile triggered by config.update
        // (in turn triggered by instance.remove) won't interfere

        lockManager.lock(new ServicesSidekickLock(Arrays.asList(service)), new LockCallbackNoReturn() {
            @Override
            public void doWithLockNoResult() {
                // 1. stop instances
                stopInstances(deploymentUnitsToStop);
                // 2. wait for reconcile (instances will be restarted along)
                reconcile(service);
            }
        });
    }

    protected void stopInstances(Map<String, List<Instance>> deploymentUnitInstancesToStop) {
        List<Instance> toStop = new ArrayList<>();
        for (String key : deploymentUnitInstancesToStop.keySet()) {
            toStop.addAll(deploymentUnitInstancesToStop.get(key));
        }
        for (Instance instance : toStop) {
            if (!instance.getState().equalsIgnoreCase(InstanceConstants.STATE_STOPPED)) {
                objectProcessMgr.scheduleProcessInstanceAsync(InstanceConstants.PROCESS_STOP,
                        instance, null);
            }
        }
        for (Instance instance : toStop) {
            resourceMntr.waitFor(instance,
                    new ResourcePredicate<Instance>() {
                        @Override
                        public boolean evaluate(Instance obj) {
                            return InstanceConstants.STATE_STOPPED.equals(obj.getState());
                        }
                    });
        }
    }

    protected void reconcile(Service service) {
        // 2. wait for reconcile (new instances will be started along)
        deploymentMgr.activate(service);
    }
}
