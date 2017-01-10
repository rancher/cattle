package io.cattle.platform.servicediscovery.deployment.impl.planner;

import io.cattle.platform.core.model.DeploymentUnit;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.Stack;
import io.cattle.platform.servicediscovery.deployment.DeploymentUnitInstanceIdGenerator;
import io.cattle.platform.servicediscovery.service.impl.DeploymentManagerImpl.DeploymentManagerContext;

import java.util.List;

public class NoOpServiceDeploymentPlanner extends AbstractServiceDeploymentPlanner {

    public NoOpServiceDeploymentPlanner(Service service, Stack stack,
            DeploymentManagerContext context) {
        super(service, context, stack);
    }

    @Override
    public List<DeploymentUnit> reconcileUnitsList(DeploymentUnitInstanceIdGenerator svcInstanceIdGenerator) {
        return getAllUnitsList();
    }

    @Override
    public boolean needToReconcileScale() {
        return false;
    }

    @Override
    protected void checkScale() {
        return;
    }
}
