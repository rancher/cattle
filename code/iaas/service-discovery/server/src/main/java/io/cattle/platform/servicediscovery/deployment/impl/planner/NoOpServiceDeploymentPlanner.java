package io.cattle.platform.servicediscovery.deployment.impl.planner;

import io.cattle.platform.core.model.Service;
import io.cattle.platform.servicediscovery.deployment.ServiceDeploymentPlanner;
import io.cattle.platform.servicediscovery.deployment.impl.DeploymentManagerImpl.DeploymentServiceContext;
import io.cattle.platform.servicediscovery.deployment.impl.unit.DeploymentUnit;

import java.util.List;

public class NoOpServiceDeploymentPlanner extends ServiceDeploymentPlanner {

    public NoOpServiceDeploymentPlanner(Service service, List<DeploymentUnit> units,
            DeploymentServiceContext context) {
        super(service, units, context);
    }

    @Override
    public List<DeploymentUnit> deployHealthyUnits() {
        return this.healthyUnits;
    }

    @Override
    public boolean needToReconcileDeploymentImpl() {
        return false;
    }
}
