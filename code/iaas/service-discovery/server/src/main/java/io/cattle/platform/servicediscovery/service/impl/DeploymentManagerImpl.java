package io.cattle.platform.servicediscovery.service.impl;

import static io.cattle.platform.core.model.tables.ServiceTable.*;
import static io.cattle.platform.core.model.tables.StackTable.*;
import io.cattle.platform.activity.ActivityService;
import io.cattle.platform.allocator.service.AllocationHelper;
import io.cattle.platform.configitem.events.ConfigUpdate;
import io.cattle.platform.configitem.model.Client;
import io.cattle.platform.configitem.request.ConfigUpdateRequest;
import io.cattle.platform.configitem.version.ConfigItemStatusManager;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.dao.HostDao;
import io.cattle.platform.core.dao.ServiceDao;
import io.cattle.platform.core.dao.ServiceExposeMapDao;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.ServiceExposeMap;
import io.cattle.platform.core.model.Stack;
import io.cattle.platform.core.util.ServiceUtil;
import io.cattle.platform.engine.idempotent.IdempotentRetryException;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.lock.LockCallback;
import io.cattle.platform.lock.LockCallbackNoReturn;
import io.cattle.platform.lock.LockManager;
import io.cattle.platform.lock.definition.LockDefinition;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.object.resource.ResourceMonitor;
import io.cattle.platform.servicediscovery.deployment.DeploymentUnitManager;
import io.cattle.platform.servicediscovery.deployment.ServiceDeploymentPlanner;
import io.cattle.platform.servicediscovery.deployment.impl.lock.ServiceLock;
import io.cattle.platform.servicediscovery.deployment.impl.planner.ServiceDeploymentPlannerFactory;
import io.cattle.platform.servicediscovery.service.DeploymentManager;
import io.cattle.platform.servicediscovery.service.ServiceDiscoveryService;
import io.cattle.platform.servicediscovery.upgrade.UpgradeManager;
import io.cattle.platform.util.exception.ServiceReconcileException;
import io.github.ibuildthecloud.gdapi.id.IdFormatter;

import java.util.Collection;
import java.util.List;

import javax.inject.Inject;

public class DeploymentManagerImpl implements DeploymentManager {

    private static final String RECONCILE = "reconcile";
    @Inject
    ActivityService activitySvc;
    @Inject
    LockManager lockManager;
    @Inject
    ObjectProcessManager objectProcessMgr;
    @Inject
    ServiceDiscoveryService sdSvc;
    @Inject
    ObjectManager objectMgr;
    @Inject
    ServiceExposeMapDao expMapDao;
    @Inject
    AllocationHelper allocationHlpr;
    @Inject
    ConfigItemStatusManager itemManager;
    @Inject
    JsonMapper mapper;
    @Inject
    HostDao hostDao;
    @Inject
    DeploymentUnitManager duManager;
    @Inject
    ServiceDao svcDao;
    @Inject
    ResourceMonitor resourceMntr;
    @Inject
    IdFormatter idFrmt;
    @Inject
    UpgradeManager upgradeMgr;

    @Override
    public boolean isHealthy(Service service) {
        return !activate(service, false);
    }

    @Override
    public void activate(final Service service) {
        activate(service, false);
    }

    /**
     * @param service
     * @return true if this service needs to be reconciled
     */
    protected boolean activate(final Service service, final boolean scheduleOnly) {
        // return immediately if inactive
        if (service == null || !ServiceUtil.isActiveService(service)) {
            return false;
        }

        return lockManager.lock(createLock(service), new LockCallback<Boolean>() {
            @Override
            public Boolean doWithLock() {
                if (!ServiceUtil.isActiveService(service)) {
                    return false;
                }
                if (!hostDao.hasActiveHosts(service.getAccountId())) {
                    objectMgr.setFields(service, SERVICE.SKIP, true);
                    return false;
                }

                objectMgr.setFields(service, SERVICE.SKIP, false);
                return deploy(service, scheduleOnly);
            }
        });
    }

    protected boolean deploy(final Service service, boolean scheduleOnly) {
        // get existing deployment units
        ServiceDeploymentPlanner planner = getPlanner(service);

        // don't process if there is no need to reconcile
        boolean needToReconcile = needToReconcile(service, planner);

        if (!needToReconcile) {
            return false;
        }

        activateService(service);
        if (scheduleOnly) {
            return false;
        }

        sdSvc.incrementExecutionCount(service);
        deployService(service, planner);

        // reload planner as there can be new hosts added for Global services
        // reload services as well
        Service reloaded = objectMgr.reload(service);
        planner = getPlanner(reloaded);
        if (needToReconcile(reloaded, planner)) {
            throw new ServiceReconcileException("Need to restart service reconcile");
        }

        activitySvc.info("Service reconciled: " + planner.getStatus());
        return false;
    }

    private boolean needToReconcile(Service service, ServiceDeploymentPlanner planner) {
        if (service.getState().equals(CommonStatesConstants.INACTIVE)) {
            return true;
        }

        return planner.needToReconcile();
    }

    private ServiceDeploymentPlanner getPlanner(Service service) {
        Stack stack = objectMgr.findOne(Stack.class, STACK.ID, service.getStackId());
        return ServiceDeploymentPlannerFactory.getServiceDeploymentPlanner(service, stack,
                new DeploymentManagerContext());
    }


    private void activateService(final Service service) {
        try {
            if (service.getState().equalsIgnoreCase(CommonStatesConstants.INACTIVE)) {
                objectProcessMgr.scheduleStandardProcess(StandardProcess.ACTIVATE, service, null);
            } else if (service.getState().equalsIgnoreCase(CommonStatesConstants.ACTIVE)) {
                objectProcessMgr.scheduleStandardProcess(StandardProcess.UPDATE, service, null);
            }
        } catch (IdempotentRetryException ex) {
        }
    }

    protected LockDefinition createLock(Service service) {
        return new ServiceLock(service);
    }

    protected void deployService(final Service service, final ServiceDeploymentPlanner planner) {
        /*
         * Deploy all the units
         */
        activitySvc.run(service, "wait", "Waiting for deployment units to activate", new Runnable() {
            @Override
            public void run() {
                planner.deploy();
            }
        });
    }


    protected void deployUnits(ServiceDeploymentPlanner planner) {
        /*
         * Ask the planner to deploy more units/ remove extra units
         */
    }

    @Override
    public void deactivate(final Service service) {
        // do with lock to prevent intervention to sidekick service activate
        lockManager.lock(createLock(service), new LockCallbackNoReturn() {
            @Override
            public void doWithLockNoResult() {
                ServiceDeploymentPlanner planner = getPlanner(service);
                planner.deactivateUnits();
            }
        });
    }

    @Override
    public void remove(final Service service) {
        // do with lock to prevent intervention to sidekick service activate
        lockManager.lock(createLock(service), new LockCallbackNoReturn() {
            @Override
            public void doWithLockNoResult() {
                ServiceDeploymentPlanner planner = getPlanner(service);
                planner.removeUnits();
                List<? extends ServiceExposeMap> unmanagedMaps = expMapDao
                        .getUnmanagedServiceInstanceMapsToRemove(service.getId());
                for (ServiceExposeMap unmanagedMap : unmanagedMaps) {
                    objectProcessMgr.scheduleStandardProcessAsync(StandardProcess.REMOVE, unmanagedMap, null);
                }
                sdSvc.removeServiceLinks(service);
                sdSvc.removeFromLoadBalancerServices(service, null);
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
        final Client client = new Client(Service.class, new Long(update.getResourceId()));
        itemManager.runUpdateForEvent(RECONCILE, update, client, new Runnable() {
            @Override
            public void run() {
                final Service service = objectMgr.loadResource(Service.class, client.getResourceId());
                if (ServiceUtil.isServiceValidForReconcile(service)) {
                    activitySvc.run(service, "service.trigger", "Re-evaluating state", new Runnable() {
                        @Override
                        public void run() {
                            activate(service, true);
                        }
                    });
                }
            }
        });
    }



    public final class DeploymentManagerContext {
        final public DeploymentUnitManager duMgr = duManager;
        final public ObjectProcessManager objectProcessManager = objectProcessMgr;
        final public ObjectManager objectManager = objectMgr;
        final public JsonMapper jsonMapper = mapper;
        final public AllocationHelper allocationHelper = allocationHlpr;
        final public ResourceMonitor resourceMonitor = resourceMntr;
        final public IdFormatter idFormatter = idFrmt;
        final public ServiceDao serviceDao = svcDao;
        final public ServiceDiscoveryService sdService = sdSvc;
    }
}
