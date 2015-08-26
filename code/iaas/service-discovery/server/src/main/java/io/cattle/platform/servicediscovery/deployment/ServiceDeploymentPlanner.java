package io.cattle.platform.servicediscovery.deployment;

import io.cattle.platform.core.model.Service;
import io.cattle.platform.servicediscovery.deployment.impl.DeploymentManagerImpl.DeploymentServiceContext;
import io.cattle.platform.servicediscovery.deployment.impl.DeploymentUnit;

import java.util.ArrayList;
import java.util.List;

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

    public ServiceDeploymentPlanner(List<Service> services, List<DeploymentUnit> units,
            DeploymentServiceContext context) {
        this.services = services;
        this.context = context;

        if (units != null) {
            for (DeploymentUnit unit : units) {
                if (unit.isError()) {
                    badUnits.add(unit);
                } else {
                    if (unit.isUnhealthy()) {
                        unhealthyUnits.add(unit);
                    } else {
                        healthyUnits.add(unit);
                    }
                    if (!unit.isComplete()) {
                        incompleteUnits.add(unit);
                    }
                }
            }
        }
    }

    public List<DeploymentUnit> deploy() {
        return this.deployHealthyUnits();
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

    public void addUnits(List<DeploymentUnit> units) {
        this.healthyUnits.addAll(units);
    }

    public List<DeploymentUnit> getBadUnits() {
        return badUnits;
    }

    public List<DeploymentUnit> getUnhealthyUnits() {
        return unhealthyUnits;
    }

    public List<Service> getServices() {
        return services;
    }

    public List<DeploymentUnit> getIncompleteUnits() {
        return incompleteUnits;
    }

    public List<DeploymentUnit> getHealthyUnits() {
        return healthyUnits;
    }
}
