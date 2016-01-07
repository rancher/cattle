package io.cattle.platform.servicediscovery.deployment.impl.healthaction;

import io.cattle.platform.servicediscovery.deployment.impl.unit.DeploymentUnit;

import java.util.List;

public class NoopHealthCheckActionHandler implements HealthCheckActionHandler {

    @Override
    public void populateHealthyUnhealthyUnits(List<DeploymentUnit> healthyUnits, List<DeploymentUnit> unhealthyUnits,
            List<DeploymentUnit> units) {
        healthyUnits.addAll(units);
    }
}
