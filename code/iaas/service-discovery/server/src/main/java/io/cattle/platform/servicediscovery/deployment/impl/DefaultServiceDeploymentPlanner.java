package io.cattle.platform.servicediscovery.deployment.impl;

import io.cattle.platform.core.model.Service;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.servicediscovery.api.constants.ServiceDiscoveryConstants;
import io.cattle.platform.servicediscovery.deployment.ServiceDeploymentPlanner;
import io.cattle.platform.servicediscovery.deployment.impl.DeploymentManagerImpl.DeploymentServiceContext;

import java.util.List;

public class DefaultServiceDeploymentPlanner extends ServiceDeploymentPlanner {

    protected Integer requestedScale = 0;

    public DefaultServiceDeploymentPlanner(List<Service> services, List<DeploymentUnit> units,
            DeploymentServiceContext context) {
        super(services, units, context);
        for (Service service : services) {
            int scale = DataAccessor.fieldInteger(service,
                    ServiceDiscoveryConstants.FIELD_SCALE);
            if (scale > this.requestedScale) {
                this.requestedScale = scale;
            }
        }
    }

    @Override
    public List<DeploymentUnit> deployHealthyUnits() {
        if (this.healthyUnits.size() < requestedScale) {
            addMissingUnits();
        } else if (healthyUnits.size() > requestedScale) {
            removeExtraUnits();
        }

        return healthyUnits;
    }

    private void addMissingUnits() {
        while (this.healthyUnits.size() < this.requestedScale) {
            DeploymentUnit unit = new DeploymentUnit(context, services, null);
            this.healthyUnits.add(unit);
        }
    }

    private void removeExtraUnits() {
        // delete units
        int i = this.healthyUnits.size() - 1;
        while (this.healthyUnits.size() > this.requestedScale) {
            DeploymentUnit toRemove = this.healthyUnits.get(i);
            toRemove.remove();
            this.healthyUnits.remove(i);
            i--;
        }
    }

    @Override
    public boolean needToReconcileDeployment() {
        return (healthyUnits.size() != requestedScale);
    }
}
