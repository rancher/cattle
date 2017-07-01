package io.cattle.platform.loop;

import io.cattle.iaas.healthcheck.service.impl.HealthcheckCleanupMonitorImpl;
import io.cattle.platform.activity.ActivityService;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.dao.HostDao;
import io.cattle.platform.core.model.DeploymentUnit;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.endpoint.loop.EndpointUpdateLoop;
import io.cattle.platform.engine.manager.LoopFactory;
import io.cattle.platform.engine.manager.LoopManager;
import io.cattle.platform.engine.model.Loop;
import io.cattle.platform.environment.EnvironmentResourceManager;
import io.cattle.platform.eventing.EventService;
import io.cattle.platform.healthcheck.loop.HealthcheckScheduleLoop;
import io.cattle.platform.inator.Deployinator;
import io.cattle.platform.lifecycle.ServiceLifecycleManager;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.systemstack.catalog.CatalogService;
import io.cattle.platform.systemstack.loop.SystemStackLoop;

import java.util.concurrent.ScheduledExecutorService;

import org.apache.commons.lang3.StringUtils;

public class LoopFactoryImpl implements LoopFactory {

    ActivityService activityService;
    CatalogService catalogService;
    Deployinator deployinator;
    EnvironmentResourceManager envResourceManager;
    EventService eventService;
    HostDao hostDao;
    LoopManager loopManager;
    ObjectManager objectManager;
    ObjectProcessManager processManager;
    ScheduledExecutorService scheduledExecutorService;
    ServiceLifecycleManager sdService;

    public LoopFactoryImpl(ActivityService activityService, CatalogService catalogService, Deployinator deployinator,
            EnvironmentResourceManager envResourceManager, EventService eventService, HostDao hostDao, LoopManager loopManager, ObjectManager objectManager,
            ObjectProcessManager processManager, ScheduledExecutorService scheduledExecutorService, ServiceLifecycleManager sdService) {
        super();
        this.activityService = activityService;
        this.catalogService = catalogService;
        this.deployinator = deployinator;
        this.envResourceManager = envResourceManager;
        this.eventService = eventService;
        this.hostDao = hostDao;
        this.loopManager = loopManager;
        this.objectManager = objectManager;
        this.processManager = processManager;
        this.scheduledExecutorService = scheduledExecutorService;
        this.sdService = sdService;
    }

    @Override
    public Loop buildLoop(String name, String type, Long id) {
        if (StringUtils.isBlank(name) || StringUtils.isBlank(type) || id == null) {
            return null;
        }

        switch (name) {
        case RECONCILE:
            Service service = objectManager.loadResource(Service.class, id);
            if (service == null) {
                return null;
            }
            return new ReconcileLoop(objectManager, processManager, deployinator, activityService, Service.class, id,
                    id, null, service.getAccountId(), ServiceConstants.KIND_SERVICE);
        case DU_RECONCILE:
            DeploymentUnit unit = objectManager.loadResource(DeploymentUnit.class, id);
            if (unit == null) {
                return null;
            }
            return new ReconcileLoop(objectManager, processManager, deployinator, activityService, DeploymentUnit.class, id,
                    unit.getServiceId(), id, unit.getAccountId(), ServiceConstants.KIND_DEPLOYMENT_UNIT);
        case HEALTHCHECK_SCHEDULE:
            return new HealthcheckScheduleLoop(id, envResourceManager, objectManager);
        case SYSTEM_STACK:
            return new SystemStackLoop(id, eventService, objectManager, hostDao, processManager, catalogService);
        case HEALTHCHECK_CLEANUP:
            return new HealthcheckCleanupMonitorImpl(id, objectManager, loopManager, scheduledExecutorService, envResourceManager);
        case ENDPOINT_UPDATE:
            return new EndpointUpdateLoop(id, envResourceManager, objectManager);
        }

        throw new IllegalArgumentException("Unknown loop " + name);
    }

}
