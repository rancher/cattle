package io.cattle.platform.servicediscovery.deployment;

import static io.cattle.platform.core.model.tables.ServiceIndexTable.SERVICE_INDEX;
import io.cattle.platform.core.addon.InstanceHealthCheck;
import io.cattle.platform.core.addon.InstanceHealthCheck.Strategy;
import io.cattle.platform.core.addon.RecreateOnQuorumStrategyConfig;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.ServiceIndex;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.servicediscovery.api.constants.ServiceDiscoveryConstants;
import io.cattle.platform.servicediscovery.api.util.ServiceDiscoveryUtil;
import io.cattle.platform.servicediscovery.deployment.impl.DeploymentManagerImpl.DeploymentServiceContext;
import io.cattle.platform.servicediscovery.deployment.impl.healthaction.HealthCheckActionHandler;
import io.cattle.platform.servicediscovery.deployment.impl.healthaction.NoopHealthCheckActionHandler;
import io.cattle.platform.servicediscovery.deployment.impl.healthaction.RecreateHealthCheckActionHandler;
import io.cattle.platform.servicediscovery.deployment.impl.healthaction.RecreateOnQuorumHealthCheckActionHandler;
import io.cattle.platform.servicediscovery.deployment.impl.unit.DeploymentUnit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * This class creates new deploymentUnits based on the service requirements (scale/global)
 * Only healthy units are taken into consideration
 * Both healthy and unhealthy units are returned (unhealthy units will get cleaned up later after the healthy ones are
 * deployed)
 *
 */
public abstract class ServiceDeploymentPlanner {

    protected List<Service> services;
    protected List<DeploymentUnit> healthyUnits = new ArrayList<>();
    private List<DeploymentUnit> unhealthyUnits = new ArrayList<>();
    private List<DeploymentUnit> badUnits = new ArrayList<>();
    private List<DeploymentUnit> incompleteUnits = new ArrayList<>();
    protected DeploymentServiceContext context;
    protected HealthCheckActionHandler healthActionHandler = new RecreateHealthCheckActionHandler();

    public ServiceDeploymentPlanner(List<Service> services, List<DeploymentUnit> units,
            DeploymentServiceContext context) {
        this.services = services;
        this.context = context;
        setHealthCheckAction(services, context);
        populateDeploymentUnits(units);
    }

    protected void populateDeploymentUnits(List<DeploymentUnit> units) {
        List<DeploymentUnit> healthyUnhealthyUnits = new ArrayList<>();
        if (units != null) {
            for (DeploymentUnit unit : units) {
                if (unit.isError()) {
                    badUnits.add(unit);
                } else {
                    healthyUnhealthyUnits.add(unit);
                    if (!unit.isComplete()) {
                        incompleteUnits.add(unit);
                    }
                }
            }
            healthActionHandler.populateHealthyUnhealthyUnits(this.healthyUnits, this.unhealthyUnits,
                    healthyUnhealthyUnits);
        }
        getUsedServiceIndexes(true);
    }

    protected void setHealthCheckAction(List<Service> services, DeploymentServiceContext context) {
        boolean set = false;
        for (Service service : services) {
            if (set) {
                break;
            }
            // get the strategy from the first service
            Object healthCheckObj = ServiceDiscoveryUtil.getLaunchConfigObject(service,
                    ServiceDiscoveryConstants.PRIMARY_LAUNCH_CONFIG_NAME, InstanceConstants.FIELD_HEALTH_CHECK);
            if (healthCheckObj != null) {
                InstanceHealthCheck healthCheck = context.jsonMapper.convertValue(healthCheckObj,
                        InstanceHealthCheck.class);
                if (healthCheck.getStrategy() == Strategy.none) {
                    healthActionHandler = new NoopHealthCheckActionHandler();
                    set = true;
                } else if (healthCheck.getStrategy() == Strategy.recreateOnQuorum) {
                    if (healthCheck
                            .getRecreateOnQuorumStrategyConfig() == null) {
                        healthCheck.setRecreateOnQuorumStrategyConfig(new RecreateOnQuorumStrategyConfig(1));
                    }
                    healthActionHandler = new RecreateOnQuorumHealthCheckActionHandler(healthCheck
                            .getRecreateOnQuorumStrategyConfig().getQuorum());
                    set = true;
                }
            }
        }
    }

    protected Map<String, List<Long>> getUsedServiceIndexes(boolean cleanupDuplicates) {
        // revamp healthy/bad units by excluding units with duplicated indexes
        Map<String, List<Long>> launchConfigToServiceIndexes = new HashMap<>();
        Iterator<DeploymentUnit> it = healthyUnits.iterator();
        while (it.hasNext()) {
            DeploymentUnit healthyUnit = it.next();
            for (DeploymentUnitInstance instance : healthyUnit.getDeploymentUnitInstances()) {
                Long serviceIndex = instance.getServiceIndex();
                if (serviceIndex == null) {
                    continue;
                }
                String launchConfigName = instance.getLaunchConfigName();
                List<Long> usedServiceIndexes = launchConfigToServiceIndexes.get(launchConfigName);
                if (usedServiceIndexes == null) {
                    usedServiceIndexes = new ArrayList<>();
                }
                if (cleanupDuplicates) {
                    if (usedServiceIndexes.contains(serviceIndex)) {
                        badUnits.add(healthyUnit);
                        it.remove();
                    }
                }

                usedServiceIndexes.add(serviceIndex);
                launchConfigToServiceIndexes.put(launchConfigName, usedServiceIndexes);
            }
        }
        return launchConfigToServiceIndexes;
    }

    public boolean isHealthcheckInitiailizing() {
        for (DeploymentUnit unit : this.getAllUnits()) {
            if (unit.isHealthCheckInitializing()) {
                return true;
            }
        }

        return false;
    }

    public List<DeploymentUnit> deploy(Map<Long, DeploymentUnitInstanceIdGenerator> svcInstanceIdGenerator) {
        List<DeploymentUnit> units = this.deployHealthyUnits();
        for (DeploymentUnit unit : units) {
            unit.start(svcInstanceIdGenerator);
        }

        for (DeploymentUnit unit : units) {
            unit.waitForStart();
        }
        return units;
    }

    protected abstract List<DeploymentUnit> deployHealthyUnits();

    public boolean needToReconcileDeployment() {
        return unhealthyUnits.size() > 0 || badUnits.size() > 0 || incompleteUnits.size() > 0
                || needToReconcileDeploymentImpl()
                || ifHealthyUnitsNeedReconcile();
    }
    
    private boolean ifHealthyUnitsNeedReconcile() {
        for (DeploymentUnit unit : healthyUnits) {
            if (!unit.isStarted()) {
                return true;
            }
        }
        return false;
    }

    protected abstract boolean needToReconcileDeploymentImpl();

    public List<Service> getServices() {
        return services;
    }

    public void cleanupBadUnits() {
        List<DeploymentUnit> watchList = new ArrayList<>();
        Iterator<DeploymentUnit> it = this.badUnits.iterator();
        while (it.hasNext()) {
            DeploymentUnit next = it.next();
            watchList.add(next);
            next.remove(false);
            it.remove();
        }
        for (DeploymentUnit toWatch : watchList) {
            toWatch.waitForRemoval();
        }
    }

    public void cleanupIncompleteUnits() {
        Iterator<DeploymentUnit> it = this.incompleteUnits.iterator();
        while (it.hasNext()) {
            DeploymentUnit next = it.next();
            next.cleanupUnit();
            it.remove();
        }
    }

    public void cleanupUnhealthyUnits() {
        List<DeploymentUnit> watchList = new ArrayList<>();
        Iterator<DeploymentUnit> it = this.unhealthyUnits.iterator();
        while (it.hasNext()) {
            DeploymentUnit next = it.next();
            watchList.add(next);
            next.remove(false);
            it.remove();
        }
        for (DeploymentUnit toWatch : watchList) {
            toWatch.waitForRemoval();
        }
    }

    private List<DeploymentUnit> getAllUnits() {
        List<DeploymentUnit> allUnits = new ArrayList<>();
        allUnits.addAll(this.healthyUnits);
        allUnits.addAll(this.unhealthyUnits);
        allUnits.addAll(this.badUnits);
        return allUnits;
    }

    public void cleanupUnusedServiceIndexes() {
        Map<String, List<Long>> launchConfigToServiceIndexes = getUsedServiceIndexes(false);

        for (ServiceIndex serviceIndex : context.objectManager.find(ServiceIndex.class, SERVICE_INDEX.SERVICE_ID, services
                .get(0).getId(), SERVICE_INDEX.REMOVED, null)) {
            boolean remove = false;
            List<Long> usedServiceIndexes = launchConfigToServiceIndexes.get(serviceIndex.getLaunchConfigName());
            if (usedServiceIndexes == null) {
                remove = true;
            } else {
                if (!usedServiceIndexes.contains(serviceIndex.getId())) {
                    remove = true;
                }
            }
            if (remove) {
                context.objectProcessManager.scheduleStandardProcess(StandardProcess.REMOVE, serviceIndex, null);
            }
        }
    }
}
