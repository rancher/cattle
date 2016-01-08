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
        for (DeploymentUnit unit : units) {
            if (unit.isUnhealthy()) {
                unhealthy.add(unit);
            } else {
                healthyUnits.add(unit);
            }
        }

        int healthyQuorum = healthyUnits.size();

        for (DeploymentUnit unhealthyUnit : unhealthy) {
            if (healthyQuorum < quorum) {
                // unhealthyUnits = units-to-recreate
                // therefore adding up one of them means increasing healthy quorum
                unhealthyUnits.add(unhealthyUnit);
                healthyQuorum++;
            } else {
                healthyUnits.add(unhealthyUnit);
            }
        }
    }
}
