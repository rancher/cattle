package io.cattle.platform.servicediscovery.process;

import io.cattle.platform.async.utils.TimeoutException;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ExitReason;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.engine.process.impl.ProcessExecutionExitException;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.resource.ResourceMonitor;
import io.cattle.platform.object.resource.ResourcePredicate;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.process.base.AbstractDefaultProcessHandler;
import io.cattle.platform.process.common.util.ProcessUtils;
import io.cattle.platform.servicediscovery.api.constants.ServiceDiscoveryConstants;
import io.cattle.platform.servicediscovery.api.dao.ServiceExposeMapDao;
import io.cattle.platform.servicediscovery.api.util.ServiceDiscoveryUtil;
import io.cattle.platform.servicediscovery.deployment.DeploymentManager;
import io.cattle.platform.servicediscovery.service.ServiceDiscoveryService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

public class ServiceUpgrade extends AbstractDefaultProcessHandler {

    private static final long SLEEP = 1000L;

    @Inject
    JsonMapper jsonMapper;

    @Inject
    DeploymentManager deploymentManager;

    @Inject
    ServiceDiscoveryService serviceDiscoveryService;

    @Inject
    ServiceExposeMapDao exposeMapDao;

    @Inject
    ResourceMonitor resourceMonitor;

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        io.cattle.platform.core.addon.ServiceUpgrade upgrade = jsonMapper.convertValue(state.getData(),
                io.cattle.platform.core.addon.ServiceUpgrade.class);
        Service service = (Service)state.getResource();

        objectManager.setFields(service, ServiceDiscoveryConstants.FIELD_UPGRADE, upgrade);

        upgrade(service, upgrade);

        return new HandlerResult(ServiceDiscoveryConstants.FIELD_UPGRADE, new Object[]{null});
    }

    private boolean isInServiceUpgrade(Service service, io.cattle.platform.core.addon.ServiceUpgrade upgrade) {
        return upgrade.getToServiceId() == null;
    }

    protected void upgrade(Service service, io.cattle.platform.core.addon.ServiceUpgrade upgrade) {
        /* TODO: move this and all downstream methods to a UpgradeManager with pluggable
         * strategies
         */
        boolean isInServiceUpgrade = isInServiceUpgrade(service, upgrade);
        if (!isInServiceUpgrade) {
            Service toService = objectManager.loadResource(Service.class, upgrade.getToServiceId());
            if (toService == null || toService.getRemoved() != null) {
                return;
            }
            updateLinks(service, upgrade);
        }

        while (!doUpgrade(service, upgrade, isInServiceUpgrade)) {
            sleep(service, upgrade);
        }
    }

    public boolean doUpgrade(Service service, io.cattle.platform.core.addon.ServiceUpgrade upgrade,
            boolean isInServiceUpgrade) {
        if (isInServiceUpgrade) {
            return doInServiceUpgrade(service, upgrade);
        } else {
            return doToServiceUpgrade(service, upgrade);
        }
    }

    protected void updateLinks(Service service, io.cattle.platform.core.addon.ServiceUpgrade upgrade) {
        if (!upgrade.isUpdateLinks()) {
            return;
        }

        serviceDiscoveryService.cloneConsumingServices(service, objectManager.loadResource(Service.class,
                upgrade.getToServiceId()));
    }

    protected Service sleep(Service service, io.cattle.platform.core.addon.ServiceUpgrade upgrade) {
        long interval = upgrade.getIntervalMillis();

        for (int i = 0 ;; i++) {
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

        if (!ServiceDiscoveryConstants.STATE_UPGRADING.equals(service.getState())) {
            throw new ProcessExecutionExitException(ExitReason.STATE_CHANGED);
        }

        return service;
    }

    protected boolean doInServiceUpgrade(Service service, io.cattle.platform.core.addon.ServiceUpgrade upgrade) {
        try {
            deploymentManager.activate(service);

            service = objectManager.reload(service);

            long batchSize = upgrade.getBatchSize();

            Map<String, List<Instance>> deploymentUnitInstancesToRemove = formDeploymentUnitsToRemove(service);

            // remove deployment units
            upgradeDeploymentUnits(batchSize, deploymentUnitInstancesToRemove, service);

            if (deploymentUnitInstancesToRemove.isEmpty()) {
                return true;
            }
            return false;

        } catch (TimeoutException e) {
            return false;
        }
    }

    protected void upgradeDeploymentUnits(long batchSize, Map<String, List<Instance>> deploymentUnitInstancesToRemove, Service service) {
        // Removal is done on per deployment unit basis
        Iterator<Map.Entry<String, List<Instance>>> it = deploymentUnitInstancesToRemove.entrySet()
                .iterator();
        long i = 0;
        while (it.hasNext() && i < batchSize) {
            List<Instance> waitList = new ArrayList<Instance>();
            Map.Entry<String, List<Instance>> instances = it.next();
            for (Instance instance : instances.getValue()) {
                objectProcessManager.scheduleProcessInstanceAsync(InstanceConstants.PROCESS_STOP,
                        instance, ProcessUtils.chainInData(new HashMap<String, Object>(),
                                InstanceConstants.PROCESS_STOP, InstanceConstants.PROCESS_REMOVE));
                waitList.add(instance);
            }
            it.remove();
            for (Instance instance : waitList) {
                resourceMonitor.waitFor(instance,
                        new ResourcePredicate<Instance>() {
                            @Override
                            public boolean evaluate(Instance obj) {
                                return CommonStatesConstants.REMOVED.equals(obj.getState());
                            }
                        });
            }
            // wait for reconcile
            deploymentManager.activate(service);
        }
    }

    protected Map<String, List<Instance>> formDeploymentUnitsToRemove(Service service) {
        List<String> launchConfigNames = ServiceDiscoveryUtil.getServiceLaunchConfigNames(service);
        Map<String, List<Instance>> deploymentUnitInstancesToRemove = new HashMap<>();
        for (String launchConfigName : launchConfigNames) {
            List<? extends Instance> instances = exposeMapDao.listServiceManagedInstances(service, launchConfigName);
            for (Instance instance : instances) {
                if (!instance.getVersion().equals(
                        ServiceDiscoveryUtil.getLaunchConfigObject(service,
                                launchConfigName,
                                ServiceDiscoveryConstants.FIELD_VERSION))) {
                    List<Instance> toRemove = deploymentUnitInstancesToRemove.get(instance.getDeploymentUnitUuid());
                    if (toRemove == null) {
                        toRemove = new ArrayList<Instance>();
                    }
                    toRemove.add(instance);
                    deploymentUnitInstancesToRemove.put(instance.getDeploymentUnitUuid(), toRemove);
                }
            }
        }
        return deploymentUnitInstancesToRemove;
    }

    /**
     * @param fromService
     * @param upgrade
     * @return true if the upgrade is done
     */
    protected boolean doToServiceUpgrade(Service fromService, io.cattle.platform.core.addon.ServiceUpgrade upgrade) {
        Service toService = objectManager.loadResource(Service.class, upgrade.getToServiceId());
        if (toService == null || toService.getRemoved() != null) {
            return true;
        }

        try {
            deploymentManager.activate(toService);
            if (!deploymentManager.isHealthy(toService)) {
                return false;
            }

            deploymentManager.activate(fromService);

            fromService = objectManager.reload(fromService);
            toService = objectManager.reload(toService);

            long batchSize = upgrade.getBatchSize();
            long finalScale = upgrade.getFinalScale();

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
        deploymentManager.activate(service);
        return objectManager.reload(service);
    }

    protected int getScale(Service service) {
        Integer i = DataAccessor.fieldInteger(service, ServiceDiscoveryConstants.FIELD_SCALE);
        return i == null ? 0 : i;
    }
}
