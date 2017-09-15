package io.cattle.platform.loop.factory;

import io.cattle.platform.activity.ActivityService;
import io.cattle.platform.agent.AgentLocator;
import io.cattle.platform.core.constants.AccountConstants;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.dao.ClusterDao;
import io.cattle.platform.core.dao.HostDao;
import io.cattle.platform.core.model.DeploymentUnit;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.engine.manager.LoopFactory;
import io.cattle.platform.engine.manager.LoopManager;
import io.cattle.platform.engine.model.Loop;
import io.cattle.platform.eventing.EventService;
import io.cattle.platform.inator.Deployinator;
import io.cattle.platform.lifecycle.ServiceLifecycleManager;
import io.cattle.platform.loop.EndpointUpdateLoop;
import io.cattle.platform.loop.HealthStateCalculateLoop;
import io.cattle.platform.loop.HealthcheckCleanupMonitorImpl;
import io.cattle.platform.loop.HealthcheckScheduleLoop;
import io.cattle.platform.loop.HostEndpointUpdateLoop;
import io.cattle.platform.loop.MetadataClientLoop;
import io.cattle.platform.loop.MetadataSyncLoop;
import io.cattle.platform.loop.ReconcileLoop;
import io.cattle.platform.loop.ServiceMembershipLoop;
import io.cattle.platform.metadata.MetadataManager;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.serialization.ObjectSerializer;
import io.cattle.platform.systemstack.catalog.CatalogService;

import java.util.concurrent.ScheduledExecutorService;

import org.apache.commons.lang3.StringUtils;

public class LoopFactoryImpl implements LoopFactory {

    AgentLocator agentLocator;
    ActivityService activityService;
    CatalogService catalogService;
    Deployinator deployinator;
    MetadataManager metadataManager;
    EventService eventService;
    HostDao hostDao;
    LoopManager loopManager;
    ObjectManager objectManager;
    ObjectProcessManager processManager;
    ObjectSerializer objectSerializer;
    ScheduledExecutorService scheduledExecutorService;
    ServiceLifecycleManager sdService;
    ClusterDao clusterDao;

    public LoopFactoryImpl(ActivityService activityService, CatalogService catalogService, Deployinator deployinator,
                           EventService eventService, HostDao hostDao, ObjectManager objectManager,
                           ObjectProcessManager processManager, ScheduledExecutorService scheduledExecutorService,
                           ServiceLifecycleManager sdService, LoopManager loopManager,
                           MetadataManager metadataManager, AgentLocator agentLocator,
                           ObjectSerializer objectSerializer, ClusterDao clusterDao) {
        super();
        this.activityService = activityService;
        this.catalogService = catalogService;
        this.deployinator = deployinator;
        this.eventService = eventService;
        this.hostDao = hostDao;
        this.objectManager = objectManager;
        this.objectSerializer = objectSerializer;
        this.processManager = processManager;
        this.scheduledExecutorService = scheduledExecutorService;
        this.sdService = sdService;
        this.loopManager = loopManager;
        this.metadataManager = metadataManager;
        this.agentLocator = agentLocator;
        this.clusterDao = clusterDao;
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
            case METADATA_CLIENT:
                return new MetadataClientLoop(id, agentLocator, metadataManager, objectManager, objectSerializer, scheduledExecutorService);
        }

        // All loops after this are for accounts
        if (!AccountConstants.TYPE.equalsIgnoreCase(type)) {
            throw new IllegalStateException("type must be account for [" + name + "] got [" + type + "]");
        }

        switch (name) {
            case HEALTHCHECK_SCHEDULE:
                return new HealthcheckScheduleLoop(id, metadataManager, objectManager);
            case HEALTHCHECK_CLEANUP:
                return new HealthcheckCleanupMonitorImpl(id, objectManager, loopManager, scheduledExecutorService, metadataManager);
            case ENDPOINT_UPDATE:
                return new EndpointUpdateLoop(id, metadataManager, objectManager, clusterDao, loopManager);
            case SERVICE_MEMBERSHIP:
                return new ServiceMembershipLoop(metadataManager, id, objectManager);
            case HEALTHSTATE_CALCULATE:
                return new HealthStateCalculateLoop(id, metadataManager, objectManager);
            case METADATA_SYNC:
                return new MetadataSyncLoop(id, loopManager);
        case HOST_ENDPOINT_UPDATE:
            return new HostEndpointUpdateLoop(id, metadataManager, objectManager);
        }

        throw new IllegalArgumentException("Unknown loop " + name);
    }

    public void setEnvResourceManager(MetadataManager metadataManager) {
        this.metadataManager = metadataManager;
    }

    public void setLoopManager(LoopManager loopManager) {
        this.loopManager = loopManager;
    }
}
