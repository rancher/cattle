package io.cattle.platform.servicediscovery.deployment.impl.planner;

import io.cattle.platform.core.model.DeploymentUnit;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.Stack;
import io.cattle.platform.servicediscovery.deployment.DeploymentUnitInstanceIdGenerator;
import io.cattle.platform.servicediscovery.deployment.ServiceDeploymentPlanner;
import io.cattle.platform.servicediscovery.deployment.impl.DeploymentManagerImpl.DeploymentManagerContext;

import java.util.List;

public class NoOpServiceDeploymentPlanner extends ServiceDeploymentPlanner {

    public NoOpServiceDeploymentPlanner(Service service, Stack stack,
            DeploymentManagerContext context) {
        super(service, context, stack);
    }

    @Override
    public List<DeploymentUnit> getUnits(DeploymentUnitInstanceIdGenerator svcInstanceIdGenerator) {
        return getAllUnitsList();
    }

    @Override
    public boolean needToReconcileScale() {
        return false;
    }
}
