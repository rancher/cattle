package io.cattle.platform.servicediscovery.healthaction;

import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.model.DeploymentUnit;
import io.cattle.platform.servicediscovery.service.impl.DeploymentManagerImpl.DeploymentManagerContext;

import java.util.Arrays;
import java.util.List;

public class RecreateHealthCheckActionHandler implements HealthCheckActionHandler {

    @Override
    public void populateHealthyUnhealthyUnits(List<DeploymentUnit> healthyUnits,
            List<DeploymentUnit> unhealthyUnits, List<DeploymentUnit> units, DeploymentManagerContext context) {
        for (DeploymentUnit unit : units) {
            if (Arrays.asList(InstanceConstants.STATE_ERROR, InstanceConstants.STATE_ERRORING)
                    .contains(unit.getState())) {
                unhealthyUnits.add(unit);
            } else {
                healthyUnits.add(unit);
            }
        }
    }
}
