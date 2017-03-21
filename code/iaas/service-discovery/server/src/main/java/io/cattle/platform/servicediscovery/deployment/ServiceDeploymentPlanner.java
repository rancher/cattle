package io.cattle.platform.servicediscovery.deployment;

import static io.cattle.platform.core.model.tables.ServiceIndexTable.*;
import io.cattle.platform.activity.ActivityLog;
import io.cattle.platform.core.addon.InstanceHealthCheck;
import io.cattle.platform.core.addon.InstanceHealthCheck.Strategy;
import io.cattle.platform.core.addon.RecreateOnQuorumStrategyConfig;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.ServiceIndex;
import io.cattle.platform.core.model.Stack;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.servicediscovery.api.util.ServiceDiscoveryUtil;
import io.cattle.platform.servicediscovery.deployment.impl.DeploymentManagerImpl.DeploymentServiceContext;
import io.cattle.platform.servicediscovery.deployment.impl.healthaction.HealthCheckActionHandler;
import io.cattle.platform.servicediscovery.deployment.impl.healthaction.NoopHealthCheckActionHandler;
import io.cattle.platform.servicediscovery.deployment.impl.healthaction.RecreateHealthCheckActionHandler;
import io.cattle.platform.servicediscovery.deployment.impl.healthaction.RecreateOnQuorumHealthCheckActionHandler;
import io.cattle.platform.servicediscovery.deployment.impl.unit.DeploymentUnit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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

    protected Service service;
    protected Stack stack;
    protected List<DeploymentUnit> healthyUnits = new ArrayList<>();
    private List<DeploymentUnit> unhealthyUnits = new ArrayList<>();
    private List<DeploymentUnit> badUnits = new ArrayList<>();
    protected List<DeploymentUnit> incompleteUnits = new ArrayList<>();
    protected DeploymentServiceContext context;
    protected HealthCheckActionHandler healthActionHandler = new RecreateHealthCheckActionHandler();

    public ServiceDeploymentPlanner(Service service, List<DeploymentUnit> units,
            DeploymentServiceContext context, Stack stack) {
        this.service = service;
        this.context = context;
        this.stack = stack;
        setHealthCheckAction(service, context);
        populateDeploymentUnits(units);
    }

    public String getStatus() {
        return String.format("Created: %d, Unhealthy: %d, Bad: %d, Incomplete: %d",
                healthyUnits.size(),
                unhealthyUnits.size(),
                badUnits.size(),
                incompleteUnits.size());
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
    }

    protected void setHealthCheckAction(Service service, DeploymentServiceContext context) {
        Object healthCheckObj = ServiceDiscoveryUtil.getLaunchConfigObject(service,
                ServiceConstants.PRIMARY_LAUNCH_CONFIG_NAME, InstanceConstants.FIELD_HEALTH_CHECK);
        if (healthCheckObj != null) {
            InstanceHealthCheck healthCheck = context.jsonMapper.convertValue(healthCheckObj,
                    InstanceHealthCheck.class);
            if (healthCheck.getStrategy() == Strategy.none) {
                healthActionHandler = new NoopHealthCheckActionHandler();
            } else if (healthCheck.getStrategy() == Strategy.recreateOnQuorum) {
                if (healthCheck
                        .getRecreateOnQuorumStrategyConfig() == null) {
                    healthCheck.setRecreateOnQuorumStrategyConfig(new RecreateOnQuorumStrategyConfig(1));
                }
                healthActionHandler = new RecreateOnQuorumHealthCheckActionHandler(healthCheck
                        .getRecreateOnQuorumStrategyConfig().getQuorum());
            }
        }
    }

    protected Map<String, List<Long>> getUsedServiceIndexesIds(boolean cleanupDuplicates) {
        // revamp healthy/bad units by excluding units with duplicated indexes
        Map<String, List<Long>> launchConfigToServiceIndexes = new HashMap<>();
        Iterator<DeploymentUnit> it = healthyUnits.iterator();
        while (it.hasNext()) {
            DeploymentUnit healthyUnit = it.next();
            for (DeploymentUnitInstance instance : healthyUnit.getDeploymentUnitInstances()) {
                if (instance.getServiceIndex() == null) {
                    continue;
                }
                Long serviceIndexId = instance.getServiceIndex().getId();
                String launchConfigName = instance.getLaunchConfigName();
                List<Long> usedServiceIndexes = launchConfigToServiceIndexes.get(launchConfigName);
                if (usedServiceIndexes == null) {
                    usedServiceIndexes = new ArrayList<>();
                }
                if (cleanupDuplicates) {
                    if (usedServiceIndexes.contains(serviceIndexId)) {
                        badUnits.add(healthyUnit);
                        it.remove();
                        break;
                    }
                }

                usedServiceIndexes.add(serviceIndexId);
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

    public List<DeploymentUnit> deploy(DeploymentUnitInstanceIdGenerator svcInstanceIdGenerator) {
        List<DeploymentUnit> units = this.deployHealthyUnits(svcInstanceIdGenerator);
        // sort based on create index
        Collections.sort(units, new Comparator<DeploymentUnit>() {
            @Override
            public int compare(DeploymentUnit d1, DeploymentUnit d2) {
                return Long.compare(d1.getCreateIndex(), d2.getCreateIndex());
            }
        });
        for (DeploymentUnit unit : units) {
            unit.create(svcInstanceIdGenerator);
        }

        for (DeploymentUnit unit : units) {
            unit.start();
        }

        for (DeploymentUnit unit : units) {
            unit.waitForStart();
        }
        return units;
    }

    protected abstract List<DeploymentUnit> deployHealthyUnits(DeploymentUnitInstanceIdGenerator svcInstanceIdGenerator);

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

    public Service getService() {
        return service;
    }

    public void cleanupBadUnits() {
        List<DeploymentUnit> watchList = new ArrayList<>();
        Iterator<DeploymentUnit> it = this.badUnits.iterator();
        while (it.hasNext()) {
            DeploymentUnit next = it.next();
            watchList.add(next);
            next.remove(ServiceConstants.AUDIT_LOG_REMOVE_BAD, ActivityLog.ERROR);
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
            next.remove(ServiceConstants.AUDIT_LOG_REMOVE_UNHEATLHY, ActivityLog.INFO);
            it.remove();
        }
        for (DeploymentUnit toWatch : watchList) {
            toWatch.waitForRemoval();
        }
    }

    protected List<DeploymentUnit> getAllUnits() {
        List<DeploymentUnit> allUnits = new ArrayList<>();
        allUnits.addAll(this.healthyUnits);
        allUnits.addAll(this.unhealthyUnits);
        allUnits.addAll(this.badUnits);
        return allUnits;
    }

    public void cleanupUnusedAndDuplicatedServiceIndexes() {
        Map<String, List<Long>> launchConfigToServiceIndexes = getUsedServiceIndexesIds(true);

        for (ServiceIndex serviceIndex : context.objectManager.find(ServiceIndex.class, SERVICE_INDEX.SERVICE_ID,
                service.getId(), SERVICE_INDEX.REMOVED, null)) {
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
                context.objectProcessManager.scheduleStandardProcessAsync(StandardProcess.REMOVE, serviceIndex, null);
            }
        }
    }
}
