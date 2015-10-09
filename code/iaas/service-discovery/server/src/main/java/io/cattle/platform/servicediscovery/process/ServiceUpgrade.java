package io.cattle.platform.servicediscovery.process;

import io.cattle.platform.async.utils.TimeoutException;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ExitReason;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.engine.process.impl.ProcessExecutionExitException;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.resource.ResourceMonitor;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.process.base.AbstractDefaultProcessHandler;
import io.cattle.platform.servicediscovery.api.constants.ServiceDiscoveryConstants;
import io.cattle.platform.servicediscovery.api.dao.ServiceExposeMapDao;
import io.cattle.platform.servicediscovery.deployment.DeploymentManager;
import io.cattle.platform.servicediscovery.service.ServiceDiscoveryService;

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
            return deploymentManager.doInServiceUpgrade(service, upgrade);
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
