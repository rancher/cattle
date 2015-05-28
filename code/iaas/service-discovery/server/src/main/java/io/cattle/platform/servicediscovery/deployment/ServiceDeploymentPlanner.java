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
    protected DeploymentServiceContext context;

    public ServiceDeploymentPlanner(List<Service> services, List<DeploymentUnit> units,
            DeploymentServiceContext context) {
        this.services = services;
        this.context = context;

        if (units != null) {
            for (DeploymentUnit unit : units) {
                if (unit.isUnhealthy()) {
                    unhealthyUnits.add(unit);
                } else {
                    healthyUnits.add(unit);
                }
            }
        }
    }

    public List<DeploymentUnit> deploy() {
        List<DeploymentUnit> allUnits = new ArrayList<>();
        allUnits.addAll(this.deployHealthyUnits());
        allUnits.addAll(unhealthyUnits);
        return allUnits;
    }
    
    public abstract List<DeploymentUnit> deployHealthyUnits();

    public abstract boolean needToReconcileDeployment();

    public void addUnits(List<DeploymentUnit> units) {
        this.healthyUnits.addAll(units);
    }
}
