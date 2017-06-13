package io.cattle.platform.servicediscovery.deployment.lookups;

import io.cattle.platform.core.model.Instance;

import java.util.Arrays;
import java.util.Collection;

public class InstanceDeploymentUnitLookup implements DeploymentUnitLookup {

    @Override
    public Collection<Long> getDeploymentUnits(Object obj) {
        if (obj instanceof Instance) {
            return Arrays.asList(((Instance) obj).getDeploymentUnitId());
        }
        return null;
    }

}
