package io.cattle.platform.servicediscovery.deployment.impl;

import io.cattle.iaas.lb.service.LoadBalancerService;
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
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.servicediscovery.api.constants.ServiceDiscoveryConstants;
import io.cattle.platform.servicediscovery.api.dao.ServiceConsumeMapDao;
import io.cattle.platform.servicediscovery.api.dao.ServiceExposeMapDao;
import io.cattle.platform.servicediscovery.deployment.DeploymentManager;
import io.cattle.platform.servicediscovery.deployment.DeploymentUnitInstance;
import io.cattle.platform.servicediscovery.deployment.DeploymentUnitInstanceFactory;
import io.cattle.platform.servicediscovery.deployment.DeploymentUnitInstanceIdGenerator;
import io.cattle.platform.servicediscovery.service.ServiceDiscoveryService;
import io.cattle.platform.storage.service.StorageService;

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
    StorageService storageSvc;
    @Inject
    ServiceConsumeMapDao consumeMapDao;

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
                List<DeploymentUnit> units = collectDeploymentUnits(services);
                int requestedScale = 0;
                for (Service service : services) {
                    int scale = DataAccessor.fieldInteger(service,
                            ServiceDiscoveryConstants.FIELD_SCALE);
                    if (scale > requestedScale) {
                        requestedScale = scale;
                    }
                }
                // don't process if there is no need to reconcile
                boolean needToReconcile = needToReconcile(services, units, requestedScale);

                if (!needToReconcile) {
                    return;
                }

                activateServices(service, services);
                activateDeploymentUnits(services, units, requestedScale);
            }
        });
    }

    private boolean needToReconcile(final List<Service> services, final List<DeploymentUnit> units, final int requestedScale) {
        for (Service service : services) {
            if (service.getState().equals(CommonStatesConstants.INACTIVE)) {
                return true;
            }
        }

        boolean needToReconcile = false;
        if (units.size() == requestedScale) {
            for (DeploymentUnit unit : units) {
                if (unit.isError()) {
                    needToReconcile = true;
                    break;
                }
                if (unit.getInstances().size() != services.size()) {
                    needToReconcile = true;
                    break;
                }
                if (!unit.isStarted()) {
                    needToReconcile = true;
                    break;
                }
                if (unit.needsCleanup()) {
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

    protected List<DeploymentUnit> collectDeploymentUnits(List<Service> services) {
        /*
         * returns a list of deployment units. Each deployment unit will have a uuid = 'io.rancher.deployment.unit'
         * label value
         */

        List<DeploymentUnit> deploymentUnits = new ArrayList<>();
        Map<String, Map<Long, DeploymentUnitInstance>> uuidToInstances = new HashMap<>();
        for (Service service : services) {
            List<DeploymentUnitInstance> deploymentUnitInstances = unitInstanceFactory
                    .collectServiceInstances(service, new DeploymentServiceContext());
            
            // group by uuid
            for (DeploymentUnitInstance deploymentUnitInstance : deploymentUnitInstances) {
                Map<Long, DeploymentUnitInstance> serviceToInstanceMap = new HashMap<>();
                if (uuidToInstances.get(deploymentUnitInstance.getUuid()) != null) {
                    serviceToInstanceMap = uuidToInstances.get(deploymentUnitInstance.getUuid());
                }
                serviceToInstanceMap.put(deploymentUnitInstance.getService().getId(), deploymentUnitInstance);
                uuidToInstances.put(deploymentUnitInstance.getUuid(), serviceToInstanceMap);
            }
        }
        for (String uuid : uuidToInstances.keySet()) {
            deploymentUnits.add(new DeploymentUnit(uuid, uuidToInstances.get(uuid), new DeploymentServiceContext()));
        }

        return deploymentUnits;
    }

    protected void activateDeploymentUnits(List<Service> services, List<DeploymentUnit> units, int requestedScale) {
        /*
         * Delete invalid units
         */
        units = deleteBadUnits(units);
        
        // get used service generated instance names - do it only after "bad" units are removed
        Map<Long, DeploymentUnitInstanceIdGenerator> svcInstanceIdGenerator = populateServiceGeneratedInstancesUsedNames(services);

        /*
         * Create new units / remove old units to match the scale
         */
        units = matchScale(services, units, requestedScale, svcInstanceIdGenerator);

        /*
         * Fill up the gaps in units
         */
        fillUpMissingUnitInstances(services, units,  svcInstanceIdGenerator);

        /*
         * Activate all the units
         */
        startUnits(units);

        /*
         * Delete the units that have a bad health
         */

        cleanup(units);
    }

    private Map<Long, DeploymentUnitInstanceIdGenerator> populateServiceGeneratedInstancesUsedNames(
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

    protected void cleanup(List<DeploymentUnit> units) {
        for (DeploymentUnit unit : units) {
            if (unit.needsCleanup()) {
                unit.cleanup();
            }
        }
    }

    protected void startUnits(List<DeploymentUnit> units) {
        for (DeploymentUnit unit : units) {
            unit.start();
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

    protected List<DeploymentUnit> matchScale(List<Service> services, List<DeploymentUnit> units, int requestedScale,
            Map<Long, DeploymentUnitInstanceIdGenerator> svcInstanceIdGenerator) {
        /*
         * If there are too many units, call unit.remove() and delete the excess.
         * If there are not enough units, create new empty deployment units by call the factory
         * and passing in the services and an empty list for instances.
         * 
         * NOTE: We don't actually start services hereed
         */

        if (units.size() < requestedScale) {
            addMissingUnits(services, units, requestedScale, svcInstanceIdGenerator);
        } else if (units.size() > requestedScale) {
            removeExtraUnits(units, requestedScale);
        }

        return units;
    }

    private void removeExtraUnits(List<DeploymentUnit> units, int scale) {
        // delete units
        int i = units.size() - 1;
        while (units.size() > scale) {
            DeploymentUnit toRemove = units.get(i);
            toRemove.remove();
            units.remove(i);
            i--;
        }
    }

    private void addMissingUnits(List<Service> services, List<DeploymentUnit> units,
            int scale, Map<Long, DeploymentUnitInstanceIdGenerator> svcInstanceIdGenerator) {
        while (units.size() < scale) {
            DeploymentUnit unit = new DeploymentUnit(services, svcInstanceIdGenerator, new DeploymentServiceContext());
            units.add(unit);
        }
    }

    private void fillUpMissingUnitInstances(List<Service> services, List<DeploymentUnit> units,
            Map<Long, DeploymentUnitInstanceIdGenerator> svcInstanceIdGenerator) {
        for (DeploymentUnit unit : units) {
            unit.createMissingUnitInstances(services, svcInstanceIdGenerator, new DeploymentServiceContext());
        }
    }

    @Override
    public void deactivate(final Service service) {
        // do with lock to prevent intervention to sidekick service activate
        lockManager.lock(createLock(Arrays.asList(service)), new LockCallbackNoReturn() {
            @Override
            public void doWithLockNoResult() {
                // in deactivate, we don't care about the sidekicks, and deactivate only requested service
                List<DeploymentUnit> units = collectDeploymentUnits(Arrays.asList(service));
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
                List<DeploymentUnit> units = collectDeploymentUnits(Arrays.asList(service));
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
        final public StorageService storageService = storageSvc;
        final DeploymentUnitInstanceFactory deploymentUnitInstanceFactory = unitInstanceFactory;
    }
}
