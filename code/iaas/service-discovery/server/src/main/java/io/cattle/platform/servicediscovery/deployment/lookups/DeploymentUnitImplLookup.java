package io.cattle.platform.servicediscovery.deployment.lookups;

import io.cattle.platform.core.model.DeploymentUnit;

import java.util.Arrays;
import java.util.Collection;

public class DeploymentUnitImplLookup implements DeploymentUnitLookup {

    @Override
    public Collection<? extends DeploymentUnit> getDeploymentUnits(Object obj) {
        if (obj instanceof DeploymentUnit) {
            return Arrays.asList((DeploymentUnit)obj);
        }
        return null;
    }

}
