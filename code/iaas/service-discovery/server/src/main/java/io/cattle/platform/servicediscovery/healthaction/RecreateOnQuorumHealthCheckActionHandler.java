package io.cattle.platform.servicediscovery.healthaction;

import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.model.DeploymentUnit;
import io.cattle.platform.servicediscovery.service.impl.DeploymentManagerImpl.DeploymentManagerContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RecreateOnQuorumHealthCheckActionHandler implements HealthCheckActionHandler {
    Integer quorum;

    public RecreateOnQuorumHealthCheckActionHandler(Integer quorum) {
        this.quorum = quorum;
    }

    @Override
    public void populateHealthyUnhealthyUnits(List<DeploymentUnit> healthyUnits,
            List<DeploymentUnit> unhealthyUnits, List<DeploymentUnit> units, DeploymentManagerContext context) {
        List<DeploymentUnit> unhealthy = new ArrayList<>();
        for (DeploymentUnit unit : units) {
            if (Arrays.asList(InstanceConstants.STATE_ERROR, InstanceConstants.STATE_ERRORING)
                    .contains(unit.getState())) {
                unhealthy.add(unit);
            } else {
                healthyUnits.add(unit);
            }
        }

        if (healthyUnits.size() >= quorum) {
            unhealthyUnits.addAll(unhealthy);
        } else {
            healthyUnits.addAll(unhealthy);
        }
    }
}
