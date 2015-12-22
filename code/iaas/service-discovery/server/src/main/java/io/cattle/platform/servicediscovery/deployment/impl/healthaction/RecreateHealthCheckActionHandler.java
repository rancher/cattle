package io.cattle.platform.servicediscovery.deployment.impl.healthaction;

import io.cattle.platform.servicediscovery.deployment.impl.unit.DeploymentUnit;

import java.util.List;

public class RecreateHealthCheckActionHandler implements HealthCheckActionHandler {

    @Override
    public void populateHealthyUnhealthyUnits(List<DeploymentUnit> healthyUnits, List<DeploymentUnit> unhealthyUnits,
            List<DeploymentUnit> units) {
        for (DeploymentUnit unit : units) {
            if (unit.isUnhealthy()) {
                unhealthyUnits.add(unit);
            } else {
                healthyUnits.add(unit);
            }
        }
    }
}
