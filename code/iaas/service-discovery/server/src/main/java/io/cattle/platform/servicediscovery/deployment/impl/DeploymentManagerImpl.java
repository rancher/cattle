package io.cattle.platform.servicediscovery.deployment.impl;

import io.cattle.iaas.lb.service.LoadBalancerService;
import io.cattle.platform.allocator.service.AllocatorService;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.dao.GenericMapDao;
import io.cattle.platform.core.model.Environment;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.engine.idempotent.IdempotentRetryException;
import io.cattle.platform.lb.instance.service.LoadBalancerInstanceManager;
import io.cattle.platform.lock.LockCallbackNoReturn;
import io.cattle.platform.lock.LockManager;
import io.cattle.platform.lock.definition.LockDefinition;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.object.resource.ResourceMonitor;
import io.cattle.platform.servicediscovery.api.dao.ServiceConsumeMapDao;
import io.cattle.platform.servicediscovery.api.dao.ServiceExposeMapDao;
import io.cattle.platform.servicediscovery.deployment.DeploymentManager;
import io.cattle.platform.servicediscovery.deployment.DeploymentUnitInstanceFactory;
import io.cattle.platform.servicediscovery.deployment.DeploymentUnitInstanceIdGenerator;
import io.cattle.platform.servicediscovery.deployment.ServiceDeploymentPlanner;
import io.cattle.platform.servicediscovery.deployment.ServiceDeploymentPlannerFactory;
import io.cattle.platform.servicediscovery.service.ServiceDiscoveryService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

public class DeploymentManagerImpl implements DeploymentManager {
    @Inject
    LockManager lockManager;
    @Inject
    DeploymentUnitInstanceFactory unitInstanceFactory;
    @Inject
    ObjectProcessManager objectProcessMgr;
    @Inject
    ServiceDiscoveryService sdSvc;
    @Inject
    LoadBalancerService lbSvc;
    @Inject
    LoadBalancerInstanceManager lbMgr;
    @Inject
    ObjectManager objectMgr;
    @Inject
    ResourceMonitor resourceMntr;
    @Inject
    ServiceExposeMapDao expMapDao;
    @Inject
    GenericMapDao mpDao;
    @Inject
    ServiceConsumeMapDao consumeMapDao;
    @Inject
    ServiceDeploymentPlannerFactory deploymentPlannerFactory;
    @Inject
    AllocatorService allocatorSvc;

    @Override
    public void activate(final Service service, final Map<String, Object> data) {
        if (service == null) {
            return;
        }
        final List<Service> services = expMapDao.collectSidekickServices(service, null);

        lockManager.lock(createLock(services), new LockCallbackNoReturn() {
            @Override
            public void doWithLockNoResult() {
                
                // get existing deployment units
                List<DeploymentUnit> units = unitInstanceFactory.collectDeploymentUnits(services,
                        new DeploymentServiceContext());
                ServiceDeploymentPlanner planner = deploymentPlannerFactory.createServiceDeploymentPlanner(services,
                        units, new DeploymentServiceContext());

                // don't process if there is no need to reconcile
                boolean needToReconcile = needToReconcile(services, units, planner);

                if (!needToReconcile) {
                    return;
                }

                activateServices(service, services);
                activateDeploymentUnits(services, units, planner);
            }
        });
    }

    private boolean needToReconcile(final List<Service> services, final List<DeploymentUnit> units,
            final ServiceDeploymentPlanner planner) {
        for (Service service : services) {
            if (service.getState().equals(CommonStatesConstants.INACTIVE)) {
                return true;
            }
        }

        boolean needToReconcile = false;
        if (!planner.needToReconcileDeployment()) {
            for (DeploymentUnit unit : units) {
                if (unit.isError()) {
                    needToReconcile = true;
                    break;
                }
                if (!unit.isComplete()) {
                    needToReconcile = true;
                    break;
                }
                if (!unit.isStarted()) {
                    needToReconcile = true;
                    break;
                }
                if (unit.isHealthy()) {
                    needToReconcile = true;
                    break;
                }
            }
        } else {
            needToReconcile = true;
        }
        return needToReconcile;
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

    protected void activateDeploymentUnits(List<Service> services, List<DeploymentUnit> units,
            ServiceDeploymentPlanner planner) {
        /*
         * Delete invalid units
         */
        units = deleteBadUnits(units);

        /*
         * Ask the planner to deploy more units/ remove extra units
         */
        units = planner.deploy();

        /*
         * Activate all the units
         */
        startUnits(units, services);

        /*
         * Delete the units that have a bad health
         */

        cleanupUnhealthyUnits(units);
    }

    private Map<Long, DeploymentUnitInstanceIdGenerator> populateUsedNames(
            List<Service> services) {
        Map<Long, DeploymentUnitInstanceIdGenerator> generator = new HashMap<>();
        for (Service service : services) {
            List<Integer> usedNames = sdSvc.getServiceInstanceUsedOrderIds(service);
            generator.put(service.getId(),
                            new DeploymentUnitInstanceIdGeneratorImpl(objectMgr.loadResource(
                                    Environment.class, service.getEnvironmentId()), service, usedNames));
        }
        return generator;
    }

    protected void cleanupUnhealthyUnits(List<DeploymentUnit> units) {
        for (DeploymentUnit unit : units) {
            if (unit.isHealthy()) {
                unit.remove();
            }
        }
    }

    protected void startUnits(List<DeploymentUnit> units, List<Service> services) {
        Map<Long, DeploymentUnitInstanceIdGenerator> svcInstanceIdGenerator = populateUsedNames(services);
        for (DeploymentUnit unit : units) {
            unit.start(svcInstanceIdGenerator);
        }
    }

    protected List<DeploymentUnit> deleteBadUnits(List<DeploymentUnit> units) {
        List<DeploymentUnit> result = new ArrayList<>(units.size());

        for (DeploymentUnit unit : units) {
            if (unit.isError()) {
                unit.remove();
            } else {
                result.add(unit);
            }
        }

        return result;
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
                List<DeploymentUnit> units = unitInstanceFactory.collectDeploymentUnits(
                        Arrays.asList(service), new DeploymentServiceContext());
                for (DeploymentUnit unit : units) {
                    unit.remove();
                }
                sdSvc.removeServiceMaps(service);
            }
        });
    }

    public final class DeploymentServiceContext {
        final public ObjectManager objectManager = objectMgr;
        final public ResourceMonitor resourceMonitor = resourceMntr;
        final public ObjectProcessManager objectProcessManager = objectProcessMgr;
        final public ServiceDiscoveryService sdService = sdSvc;
        final public ServiceExposeMapDao exposeMapDao = expMapDao;
        final public LoadBalancerInstanceManager lbInstanceMgr = lbMgr;
        final public LoadBalancerService lbService = lbSvc;
        final public DeploymentUnitInstanceFactory deploymentUnitInstanceFactory = unitInstanceFactory;
        final public AllocatorService allocatorService = allocatorSvc;
    }
}
