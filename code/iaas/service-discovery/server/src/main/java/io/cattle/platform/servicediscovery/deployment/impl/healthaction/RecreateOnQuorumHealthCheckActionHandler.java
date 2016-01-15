package io.cattle.platform.servicediscovery.deployment.impl.healthaction;

import io.cattle.platform.servicediscovery.deployment.impl.unit.DeploymentUnit;

import java.util.ArrayList;
import java.util.List;

public class RecreateOnQuorumHealthCheckActionHandler implements HealthCheckActionHandler {
    Integer quorum;

    public RecreateOnQuorumHealthCheckActionHandler(Integer quorum) {
        this.quorum = quorum;
    }

    @Override
    public void populateHealthyUnhealthyUnits(List<DeploymentUnit> healthyUnits, List<DeploymentUnit> unhealthyUnits, List<DeploymentUnit> units) {
        List<DeploymentUnit> unhealthy = new ArrayList<>();
        List<DeploymentUnit> initializing = new ArrayList<>();
        for (DeploymentUnit unit : units) {
            if (unit.isUnhealthy()) {
                unhealthy.add(unit);
            } else if (unit.isHealthCheckInitializing()) {
                initializing.add(unit);
            } else {
                healthyUnits.add(unit);
            }
        }

        if (healthyUnits.size() >= quorum) {
            unhealthyUnits.addAll(unhealthy);
        } else {
            healthyUnits.addAll(unhealthy);
        }
        healthyUnits.addAll(initializing);
    }
}
