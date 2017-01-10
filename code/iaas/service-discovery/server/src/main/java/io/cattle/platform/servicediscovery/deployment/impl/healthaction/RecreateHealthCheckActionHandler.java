package io.cattle.platform.servicediscovery.deployment.impl.healthaction;

import io.cattle.platform.core.model.DeploymentUnit;
import io.cattle.platform.servicediscovery.deployment.impl.DeploymentManagerImpl.DeploymentManagerContext;

import java.util.List;

public class RecreateHealthCheckActionHandler implements HealthCheckActionHandler {

    @Override
    public void populateHealthyUnhealthyUnits(List<DeploymentUnit> healthyUnits,
            List<DeploymentUnit> unhealthyUnits, List<DeploymentUnit> units, DeploymentManagerContext context) {
        for (DeploymentUnit unit : units) {
            if (context.duMgr.isUnhealthy(unit)) {
                unhealthyUnits.add(unit);
            } else {
                healthyUnits.add(unit);
            }
        }
    }
}
