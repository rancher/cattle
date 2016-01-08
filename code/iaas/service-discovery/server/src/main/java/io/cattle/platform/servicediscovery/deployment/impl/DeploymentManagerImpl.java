package io.cattle.platform.servicediscovery.deployment.impl;

import io.cattle.platform.allocator.service.AllocatorService;
import io.cattle.platform.configitem.events.ConfigUpdate;
import io.cattle.platform.configitem.model.Client;
import io.cattle.platform.configitem.model.ItemVersion;
import io.cattle.platform.configitem.request.ConfigUpdateRequest;
import io.cattle.platform.configitem.version.ConfigItemStatusManager;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.ServiceExposeMap;
import io.cattle.platform.engine.idempotent.IdempotentRetryException;
import io.cattle.platform.eventing.EventService;
import io.cattle.platform.eventing.model.EventVO;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.lock.LockCallback;
import io.cattle.platform.lock.LockCallbackNoReturn;
import io.cattle.platform.lock.LockManager;
import io.cattle.platform.lock.definition.LockDefinition;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.object.resource.ResourceMonitor;
import io.cattle.platform.servicediscovery.api.dao.ServiceExposeMapDao;
import io.cattle.platform.servicediscovery.api.util.ServiceDiscoveryUtil;
import io.cattle.platform.servicediscovery.deployment.DeploymentManager;
import io.cattle.platform.servicediscovery.deployment.DeploymentUnitInstanceFactory;
import io.cattle.platform.servicediscovery.deployment.DeploymentUnitInstanceIdGenerator;
import io.cattle.platform.servicediscovery.deployment.ServiceDeploymentPlanner;
import io.cattle.platform.servicediscovery.deployment.ServiceDeploymentPlannerFactory;
import io.cattle.platform.servicediscovery.deployment.impl.lock.ServicesSidekickLock;
import io.cattle.platform.servicediscovery.deployment.impl.unit.DeploymentUnit;
import io.cattle.platform.servicediscovery.deployment.impl.unit.DeploymentUnitInstanceIdGeneratorImpl;
import io.cattle.platform.servicediscovery.service.ServiceDiscoveryService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

public class DeploymentManagerImpl implements DeploymentManager {

    private static final String RECONCILE = "reconcile";

    @Inject
    LockManager lockManager;
    @Inject
    DeploymentUnitInstanceFactory unitInstanceFactory;
    @Inject
    ObjectProcessManager objectProcessMgr;
    @Inject
    ServiceDiscoveryService sdSvc;
    @Inject
    ObjectManager objectMgr;
    @Inject
    ResourceMonitor resourceMntr;
    @Inject
    ServiceExposeMapDao expMapDao;
    @Inject
    ServiceDeploymentPlannerFactory deploymentPlannerFactory;
    @Inject
    AllocatorService allocatorSvc;
    @Inject
    ConfigItemStatusManager itemManager;
    @Inject
    EventService eventService;
    @Inject
    JsonMapper mapper;


    @Override
    public boolean isHealthy(Service service) {
        return !activate(service, true);
    }

    @Override
    public void activate(final Service service) {
        activate(service, false);
    }

    /**
     * @param service
     * @param checkState
     * @return true if this service needs to be reconciled
     */
    protected boolean activate(final Service service, final boolean checkState) {
        // return immediately if inactive
        if (service == null || !sdSvc.isActiveService(service)) {
            return false;
        }

        final List<Service> services = new ArrayList<>();
        services.add(service);

        return lockManager.lock(checkState ? null : createLock(services), new LockCallback<Boolean>() {
            @Override
            public Boolean doWithLock() {
                if (!sdSvc.isActiveService(service)) {
                    return false;
                }
                // get existing deployment units
                ServiceDeploymentPlanner planner = getPlanner(services);

                // don't process if there is no need to reconcile
                boolean needToReconcile = needToReconcile(services, planner);

                if (!needToReconcile) {
                    return false;
                }

                if (checkState) {
                    return !planner.isHealthcheckInitiailizing();
                }

                activateServices(service, services);
                activateDeploymentUnits(planner);

                // reload planner as there can be new hosts added for Global services
                planner = getPlanner(services);
                if (needToReconcile(services, planner)) {
                    throw new IllegalStateException(
                            "Failed to do service reconcile for service [" + service.getId() + "]");
                }

                return false;
            }
        });
    }
    
    private ServiceDeploymentPlanner getPlanner(List<Service> services) {
        List<DeploymentUnit> units = unitInstanceFactory.collectDeploymentUnits(services,
                new DeploymentServiceContext());
        return deploymentPlannerFactory.createServiceDeploymentPlanner(services,
                units, new DeploymentServiceContext());
    }

    private boolean needToReconcile(List<Service> services, ServiceDeploymentPlanner planner) {
        for (Service service : services) {
            if (service.getState().equals(CommonStatesConstants.INACTIVE)) {
                return true;
            }
        }

        return planner.needToReconcileDeployment();
    }

    private void activateServices(final Service initialService, final List<Service> services) {
        /*
         * Trigger activate for all the services
         */
        try {
            for (Service service : services) {
                if (service.getState().equalsIgnoreCase(CommonStatesConstants.INACTIVE)) {
                    objectProcessMgr.scheduleStandardProcess(StandardProcess.ACTIVATE, service, null);
                } else if (service.getState().equalsIgnoreCase(CommonStatesConstants.ACTIVE)) {
                    objectProcessMgr.scheduleStandardProcess(StandardProcess.UPDATE, service, null);
                }
            }
        } catch (IdempotentRetryException ex) {
            // if not caught, the process will keep on spinning forever
            // figure out better solution
        }

    }

    protected LockDefinition createLock(List<Service> services) {
        return new ServicesSidekickLock(services);
    }

    protected void activateDeploymentUnits(ServiceDeploymentPlanner planner) {
        /*
         * Delete invalid units
         */
        planner.cleanupBadUnits();

        /*
         * Cleanup incomplete units
         */
        planner.cleanupIncompleteUnits();

        /*
         * Activate all the units
         */
        startUnits(planner);

        /*
         * Delete the units that have a bad health
         */
        planner.cleanupUnhealthyUnits();
    }

    private Map<Long, DeploymentUnitInstanceIdGenerator> populateUsedNames(
            List<Service> services) {
        Map<Long, DeploymentUnitInstanceIdGenerator> generator = new HashMap<>();
        for (Service service : services) {
            Map<String, List<Integer>> launchConfigUsedIds = new HashMap<>();
            for (String launchConfigName : ServiceDiscoveryUtil.getServiceLaunchConfigNames(service)) {
                List<Integer> usedIds = sdSvc.getServiceInstanceUsedOrderIds(service, launchConfigName);
                launchConfigUsedIds.put(launchConfigName, usedIds);
            }
            generator.put(service.getId(),
                    new DeploymentUnitInstanceIdGeneratorImpl(launchConfigUsedIds));
        }
        return generator;
    }

    protected void startUnits(ServiceDeploymentPlanner planner) {
        Map<Long, DeploymentUnitInstanceIdGenerator> svcInstanceIdGenerator = populateUsedNames(planner.getServices());
        /*
         * Ask the planner to deploy more units/ remove extra units
         */
        planner.deploy(svcInstanceIdGenerator);
    }

    @Override
    public void deactivate(final Service service) {
        // do with lock to prevent intervention to sidekick service activate
        lockManager.lock(createLock(Arrays.asList(service)), new LockCallbackNoReturn() {
            @Override
            public void doWithLockNoResult() {
                // in deactivate, we don't care about the sidekicks, and deactivate only requested service
                List<DeploymentUnit> units = unitInstanceFactory.collectDeploymentUnits(
                        Arrays.asList(service), new DeploymentServiceContext());
                for (DeploymentUnit unit : units) {
                    unit.stop();
                }
            }
        });
    }

    @Override
    public void remove(final Service service) {
        // do with lock to prevent intervention to sidekick service activate
        lockManager.lock(createLock(Arrays.asList(service)), new LockCallbackNoReturn() {
            @Override
            public void doWithLockNoResult() {
                // in remove, we don't care about the sidekicks, and remove only requested service
                deleteServiceInstances(service);
                List<? extends ServiceExposeMap> unmanagedMaps = expMapDao
                        .getUnmanagedServiceInstanceMapsToRemove(service.getId());
                for (ServiceExposeMap unmanagedMap : unmanagedMaps) {
                    objectProcessMgr.scheduleStandardProcessAsync(StandardProcess.REMOVE, unmanagedMap, null);
                }
                sdSvc.removeServiceMaps(service);
            }

            protected void deleteServiceInstances(final Service service) {
                List<DeploymentUnit> units = unitInstanceFactory.collectDeploymentUnits(
                        Arrays.asList(service), new DeploymentServiceContext());
                for (DeploymentUnit unit : units) {
                    unit.remove(false);
                }
            }
        });
    }


    @Override
    public void reconcileServices(Collection<? extends Service> services) {
        for (Service service: services) {
            ConfigUpdateRequest request = ConfigUpdateRequest.forResource(Service.class, service.getId());
            request.addItem(RECONCILE);
            request.withDeferredTrigger(true);
            itemManager.updateConfig(request);
        }
    }

    @Override
    public void serviceUpdate(ConfigUpdate update) {
        if (update.getResourceId() == null) {
            return;
        }

        final Client client = new Client(Service.class, new Long(update.getResourceId()));
        reconcileForClient(update, client, new Runnable() {
            @Override
            public void run() {
                Service service = objectMgr.loadResource(Service.class, client.getResourceId());
                if (service != null && service.getState().equalsIgnoreCase(CommonStatesConstants.ACTIVE)) {
                    activate(service);
                }
            }
        });
    }

    protected void reconcileForClient(ConfigUpdate update, Client client, Runnable run) {
        ItemVersion itemVersion = itemManager.getRequestedVersion(client, RECONCILE);
        run.run();
        itemManager.setApplied(client, RECONCILE, itemVersion);
        eventService.publish(EventVO.reply(update));
    }

    public final class DeploymentServiceContext {
        final public ObjectManager objectManager = objectMgr;
        final public ResourceMonitor resourceMonitor = resourceMntr;
        final public ObjectProcessManager objectProcessManager = objectProcessMgr;
        final public ServiceDiscoveryService sdService = sdSvc;
        final public ServiceExposeMapDao exposeMapDao = expMapDao;
        final public DeploymentUnitInstanceFactory deploymentUnitInstanceFactory = unitInstanceFactory;
        final public AllocatorService allocatorService = allocatorSvc;
        final public JsonMapper jsonMapper = mapper;
    }
}
