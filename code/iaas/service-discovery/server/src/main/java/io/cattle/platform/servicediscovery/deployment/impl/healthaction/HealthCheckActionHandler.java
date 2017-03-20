package io.cattle.platform.servicediscovery.deployment.impl.healthaction;

import io.cattle.platform.servicediscovery.deployment.impl.unit.DeploymentUnit;

import java.util.List;

public interface HealthCheckActionHandler {

    void populateHealthyUnhealthyUnits(List<DeploymentUnit> healthyUnits, List<DeploymentUnit> unhealthyUnits, List<DeploymentUnit> units);

}
