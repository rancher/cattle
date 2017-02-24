package io.cattle.platform.servicediscovery.deployment.impl;

import static io.cattle.platform.core.model.tables.ServiceTable.*;
import static io.cattle.platform.core.model.tables.StackTable.*;
import io.cattle.platform.activity.ActivityService;
import io.cattle.platform.allocator.service.AllocationHelper;
import io.cattle.platform.configitem.events.ConfigUpdate;
import io.cattle.platform.configitem.model.Client;
import io.cattle.platform.configitem.request.ConfigUpdateRequest;
import io.cattle.platform.configitem.version.ConfigItemStatusManager;
import io.cattle.platform.core.addon.ScalePolicy;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.dao.HostDao;
import io.cattle.platform.core.dao.ServiceDao;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.ServiceExposeMap;
import io.cattle.platform.core.model.Stack;
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
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.servicediscovery.api.dao.ServiceExposeMapDao;
import io.cattle.platform.servicediscovery.api.util.ServiceDiscoveryUtil;
import io.cattle.platform.servicediscovery.deployment.DeploymentManager;
import io.cattle.platform.servicediscovery.deployment.DeploymentUnitManager;
import io.cattle.platform.servicediscovery.deployment.ServiceDeploymentPlanner;
import io.cattle.platform.servicediscovery.deployment.impl.lock.ServiceLock;
import io.cattle.platform.servicediscovery.deployment.impl.planner.DefaultServiceDeploymentPlanner;
import io.cattle.platform.servicediscovery.deployment.impl.planner.GlobalServiceDeploymentPlanner;
import io.cattle.platform.servicediscovery.deployment.impl.planner.NoOpServiceDeploymentPlanner;
import io.cattle.platform.servicediscovery.service.ServiceDiscoveryService;
import io.cattle.platform.servicediscovery.upgrade.UpgradeManager;
import io.cattle.platform.util.exception.DeploymentUnitAllocateException;
import io.cattle.platform.util.exception.ServiceReconcileException;
import io.github.ibuildthecloud.gdapi.id.IdFormatter;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeploymentManagerImpl implements DeploymentManager {

    private static final String RECONCILE = "reconcile";
    private static final Logger log = LoggerFactory.getLogger(DeploymentManagerImpl.class);
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
        return !activate(service, true, false);
    }

    @Override
    public void activate(final Service service) {
        activate(service, false, false);
    }

    /**
     * @param service
     * @param checkState
     * @return true if this service needs to be reconciled
     */
    protected boolean activate(final Service service, final boolean checkState, final boolean scheduleOnly) {
        // return immediately if inactive
        if (service == null || !sdSvc.isActiveService(service)) {
            return false;
        }

        return lockManager.lock(checkState ? null : createLock(service), new LockCallback<Boolean>() {
            @Override
            public Boolean doWithLock() {
                if (!sdSvc.isActiveService(service)) {
                    return false;
                }
                if (!hostDao.hasActiveHosts(service.getAccountId())) {
                    objectMgr.setFields(service, SERVICE.SKIP, true);
                    return false;
                }

                objectMgr.setFields(service, SERVICE.SKIP, false);
                return reconcileDeployment(service, checkState, scheduleOnly);
            }
        });
    }

    protected boolean reconcileDeployment(final Service service, final boolean checkState, boolean scheduleOnly) {
        ScalePolicy policy = DataAccessor.field(service,
                ServiceConstants.FIELD_SCALE_POLICY, mapper, ScalePolicy.class);
        boolean result = false;
        if (policy == null) {
            result = deploy(service, checkState, scheduleOnly);
        } else {
            result = deployWithScaleAdjustement(service, checkState, policy);
        }
        return result;
    }

    protected boolean deployWithScaleAdjustement(final Service service, final boolean checkState,
            ScalePolicy policy) {

        Integer desiredScaleToReset = null;
        Integer desiredScaleSet = DataAccessor.fieldInteger(service,
                ServiceConstants.FIELD_DESIRED_SCALE);
        if (desiredScaleSet == null) {
            desiredScaleToReset = policy.getMin();
        } else if (desiredScaleSet.intValue() > policy.getMax().intValue()) {
            desiredScaleToReset = policy.getMax();
        }

        boolean initLockScale = false;
        if (DataAccessor.fieldInteger(service,
                ServiceConstants.FIELD_LOCKED_SCALE) == null) {
            initLockScale = true;
        }

        if (desiredScaleToReset != null) {
            initLockScale = true;
            desiredScaleToReset = setDesiredScaleInternal(service, desiredScaleToReset);
            log.info("Set service [{}] desired scale to [{}]", service.getUuid(), desiredScaleToReset);
        }

        if (initLockScale) {
            lockScale(service);
        }

        return incremenetScaleAndDeploy(service, checkState, policy);
    }

    protected boolean incremenetScaleAndDeploy(final Service service, final boolean checkState,
            ScalePolicy policy) {
        Integer desiredScale = DataAccessor.fieldInteger(service,
                ServiceConstants.FIELD_DESIRED_SCALE);
        try {
            deploy(service, checkState, false);
            lockScale(service);
        } catch (DeploymentUnitAllocateException ex) {
            reduceScaleAndDeploy(service, checkState, policy);
            return false;
        }
        if (desiredScale.intValue() < policy.getMax().intValue()) {
            Integer newDesiredScale = policy.getMax() - desiredScale < policy.getIncrement().intValue() ? policy
                    .getMax()
                    : desiredScale
                            + policy.getIncrement();
            desiredScale = setDesiredScaleInternal(service, newDesiredScale);
            log.info("Incremented service [{}] scale to [{}] as reconcile has succeed", service.getUuid(),
                    desiredScale);
            incremenetScaleAndDeploy(service, checkState, policy);
        }

        return false;
    }

    protected boolean reduceScaleAndDeploy(Service service, boolean checkState, ScalePolicy policy) {
        int desiredScale = DataAccessor.fieldInteger(service, ServiceConstants.FIELD_DESIRED_SCALE).intValue();
        int lockedScale = DataAccessor.fieldInteger(service, ServiceConstants.FIELD_LOCKED_SCALE).intValue();
        int minScale = policy.getMin();
        // account for the fact that scale policy can be updated
        if (lockedScale > policy.getMin().intValue()) {
            minScale = lockedScale;
        }
        if (lockedScale >= policy.getMax().intValue()) {
            minScale = policy.getMax();
        }

        int increment = policy.getIncrement().intValue();
        if (desiredScale >= minScale) {
            // reduce scale by interval and try to deploy again
            Integer newDesiredScale = desiredScale - increment <= minScale ? minScale : desiredScale
                    - policy.getIncrement();
            desiredScale = setDesiredScaleInternal(service, newDesiredScale);
            log.info("Decremented service [{}] scale to [{}] as reconcile has failed", service.getUuid(), desiredScale);
            try {
                deploy(service, checkState, false);
            } catch (DeploymentUnitAllocateException ex) {
                if (desiredScale == minScale) {
                    throw ex;
                }
                reduceScaleAndDeploy(service, checkState, policy);
            }
        }
        return false;
    }

    protected Integer setDesiredScaleInternal(Service service, Integer newScale) {
        service = objectMgr.reload(service);
        Map<String, Object> data = new HashMap<>();
        data.put(ServiceConstants.FIELD_DESIRED_SCALE, newScale);
        objectMgr.setFields(service, data);
        return newScale;
    }

    protected void lockScale(Service service) {
        Integer desiredScale = DataAccessor.fieldInteger(service,
                ServiceConstants.FIELD_DESIRED_SCALE);
        service = objectMgr.reload(service);
        Map<String, Object> data = new HashMap<>();
        data.put(ServiceConstants.FIELD_LOCKED_SCALE, desiredScale);
        objectMgr.setFields(service, data);
    }

    protected boolean deploy(final Service service, final boolean checkState, boolean scheduleOnly) {
        // get existing deployment units
        ServiceDeploymentPlanner planner = getPlanner(service);

        if (!checkState) {
            activitySvc.info(planner.getStatus());
        }

        // don't process if there is no need to reconcile
        boolean needToReconcile = needToReconcile(service, planner);

        if (!needToReconcile) {
            if (!checkState) {
                activitySvc.info("Service already reconciled");
            }
            return false;
        }

        if (checkState) {
            return !planner.isHealthcheckInitiailizing();
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



    private ServiceDeploymentPlanner getPlanner(Service service) {
        return createServiceDeploymentPlanner(service);
    }

    private boolean needToReconcile(Service service, ServiceDeploymentPlanner planner) {
        if (service.getState().equals(CommonStatesConstants.INACTIVE)) {
            return true;
        }

        return planner.needToReconcileDeployment();
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
                if (sdSvc.isServiceValidForReconcile(service)) {
                    activitySvc.run(service, "service.trigger", "Re-evaluating state", new Runnable() {
                        @Override
                        public void run() {
                            activate(service, false, true);
                        }
                    });
                }
            }
        });
    }

    public ServiceDeploymentPlanner createServiceDeploymentPlanner(Service service) {
        if (service == null) {
            return null;
        }
        Stack stack = objectMgr.findOne(Stack.class, STACK.ID, service.getStackId());
        boolean isGlobalDeploymentStrategy = isGlobalDeploymentStrategy(service);
        boolean isSelectorOnlyStrategy = isNoopStrategy(service);
        if (isSelectorOnlyStrategy
                || service.getKind().equalsIgnoreCase(ServiceConstants.KIND_DNS_SERVICE)
                || service.getKind().equalsIgnoreCase(ServiceConstants.KIND_EXTERNAL_SERVICE)) {
            return new NoOpServiceDeploymentPlanner(service, stack, new DeploymentManagerContext());
        } else if (isGlobalDeploymentStrategy) {
            return new GlobalServiceDeploymentPlanner(service, stack, new DeploymentManagerContext());
        } else {
            return new DefaultServiceDeploymentPlanner(service, stack, new DeploymentManagerContext());
        }
    }

    protected boolean isGlobalDeploymentStrategy(Service service) {
        return sdSvc.isGlobalService(service);
    }

    protected boolean isNoopStrategy(Service service) {
        if (ServiceDiscoveryUtil.isNoopService(service) || isExternallyProvidedService(service)) {
            return true;
        }
        return false;
    }

    protected boolean isExternallyProvidedService(Service service) {
        if (service.getKind().equalsIgnoreCase(ServiceConstants.KIND_DNS_SERVICE)
                || service.getKind().equalsIgnoreCase(ServiceConstants.KIND_EXTERNAL_SERVICE)
                || ServiceConstants.SERVICE_LIKE.contains(service.getKind())) {
            return false;
        }
        return true;
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
    }
}
