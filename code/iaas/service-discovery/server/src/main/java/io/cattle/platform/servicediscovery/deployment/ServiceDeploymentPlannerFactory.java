package io.cattle.platform.servicediscovery.deployment;

import io.cattle.platform.core.model.Service;
import io.cattle.platform.servicediscovery.deployment.impl.DeploymentManagerImpl.DeploymentServiceContext;
import io.cattle.platform.servicediscovery.deployment.impl.unit.DeploymentUnit;

import java.util.List;

public interface ServiceDeploymentPlannerFactory {

    ServiceDeploymentPlanner createServiceDeploymentPlanner(Service service, List<DeploymentUnit> units,
            DeploymentServiceContext context);
}
